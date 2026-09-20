-- AMAN Phase 3 correction: reconcile only historical task completions with an
-- explicit telecom reference; never invent a reference for legacy rows.

BEGIN;

INSERT INTO public.manual_payment_logs(task_id, amount, telecom_reference, notes, logged_by, created_at)
SELECT t.id, coalesce(t.amount_snapshot,0), trim(t.telecom_reference), 'ترحيل سجل تنفيذ قديم', t.completed_by, coalesce(t.completed_at, now())
FROM public.payment_tasks t
WHERE t.status='completed'::public.task_status
  AND nullif(trim(t.telecom_reference), '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.manual_payment_logs l WHERE l.task_id=t.id);

INSERT INTO public.financial_transactions(type, amount, currency, reference_id, notes, created_by, created_at)
SELECT 'telecom_renewal_expense'::public.transaction_type, coalesce(t.amount_snapshot,0), coalesce(t.amount_currency,'YER'),
       t.id, 'ترحيل معاملة تنفيذ مهمة قديمة', t.completed_by, coalesce(t.completed_at, now())
FROM public.payment_tasks t
WHERE t.status='completed'::public.task_status
  AND nullif(trim(t.telecom_reference), '') IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM public.financial_transactions f
    WHERE f.reference_id=t.id AND f.type='telecom_renewal_expense'::public.transaction_type
  );

CREATE OR REPLACE FUNCTION public.complete_payment_task(p_task_id uuid, p_telecom_ref text DEFAULT NULL)
RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_task record;
  v_provider uuid;
  v_interval integer;
  v_visibility integer;
  v_next timestamptz;
  v_next_amount numeric;
  v_next_currency text;
  v_ref text;
BEGIN
  IF NOT private.has_permission('tasks.complete') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  SELECT * INTO v_task FROM public.payment_tasks WHERE id=p_task_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'TASK_NOT_FOUND'; END IF;
  -- Safe retry: a task that is already completed does not create any duplicate records.
  IF v_task.status='completed'::public.task_status THEN RETURN true; END IF;
  IF v_task.status='cancelled'::public.task_status THEN RAISE EXCEPTION 'TASK_CANCELLED'; END IF;
  IF v_task.status NOT IN ('open'::public.task_status, 'due'::public.task_status) THEN RAISE EXCEPTION 'INVALID_TASK_STATUS'; END IF;
  v_ref := nullif(trim(coalesce(p_telecom_ref, '')), '');
  IF v_ref IS NULL THEN RAISE EXCEPTION 'TELECOM_REFERENCE_REQUIRED'; END IF;

  SELECT coalesce(v_task.provider_id,pn.provider_id) INTO v_provider
  FROM public.protections p JOIN public.customer_numbers cn ON cn.id=p.customer_number_id JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id
  WHERE p.id=v_task.protection_id;
  IF v_provider IS NULL THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;

  UPDATE public.payment_tasks
  SET status='completed',completed_at=now(),completed_by=auth.uid(),telecom_reference=v_ref,updated_at=now()
  WHERE id=p_task_id;

  INSERT INTO public.manual_payment_logs(task_id,amount,telecom_reference,notes,logged_by)
  VALUES(p_task_id,coalesce(v_task.amount_snapshot,0),v_ref,nullif(v_task.rescheduled_reason,''),auth.uid())
  ON CONFLICT (task_id) DO NOTHING;

  INSERT INTO public.financial_transactions(type,amount,currency,reference_id,notes,created_by)
  VALUES('telecom_renewal_expense'::public.transaction_type,coalesce(v_task.amount_snapshot,0),coalesce(v_task.amount_currency,'YER'),p_task_id,'مصروف تنفيذ مهمة دفع',auth.uid())
  ON CONFLICT (reference_id,type) DO NOTHING;

  SELECT coalesce(default_interval_days,90),coalesce(visibility_days_before,30)
    INTO v_interval,v_visibility
  FROM public.task_settings WHERE provider_id=v_provider AND is_active LIMIT 1;
  v_interval:=coalesce(v_interval,90); v_visibility:=coalesce(v_visibility,30);
  v_next:=coalesce(v_task.planned_due_at,v_task.due_date)+(v_interval||' days')::interval;

  SELECT amount,currency INTO v_next_amount,v_next_currency
  FROM public.task_amount_settings
  WHERE provider_id=v_provider AND task_type='recurring' AND is_active LIMIT 1;

  IF EXISTS (SELECT 1 FROM public.protections WHERE id=v_task.protection_id AND status='active'::public.protection_status AND expires_at>v_next) THEN
    INSERT INTO public.payment_tasks(protection_id,provider_id,task_type,due_date,telecom_due_at,planned_due_at,visible_from,status,amount_snapshot,amount_currency,cycle_number)
    VALUES(v_task.protection_id,v_provider,'recurring',v_next,v_next,v_next,v_next-(v_visibility||' days')::interval,'open'::public.task_status,coalesce(v_next_amount,0),coalesce(v_next_currency,'YER'),coalesce(v_task.cycle_number,1)+1)
    ON CONFLICT (protection_id,task_type,planned_due_at) DO NOTHING;
  END IF;

  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'complete_payment_task','payment_task',p_task_id,jsonb_build_object('amount_snapshot',v_task.amount_snapshot,'telecom_reference',v_ref,'next_due_at',v_next));
  RETURN true;
END;
$$;

COMMIT;
