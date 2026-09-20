-- AMAN Phase 1 correction: task operating amounts are independent from package prices.

BEGIN;

CREATE TABLE IF NOT EXISTS public.task_amount_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  provider_id uuid NOT NULL REFERENCES public.telecom_providers(id),
  task_type text NOT NULL,
  amount numeric NOT NULL DEFAULT 0,
  currency text NOT NULL DEFAULT 'YER',
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT task_amount_settings_type_ck CHECK (nullif(trim(task_type), '') IS NOT NULL),
  CONSTRAINT task_amount_settings_amount_ck CHECK (amount >= 0),
  CONSTRAINT task_amount_settings_currency_ck CHECK (nullif(trim(currency), '') IS NOT NULL),
  CONSTRAINT task_amount_settings_unique_ck UNIQUE (provider_id, task_type)
);

ALTER TABLE public.task_amount_settings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS task_amount_settings_admin_read ON public.task_amount_settings;
CREATE POLICY task_amount_settings_admin_read ON public.task_amount_settings
  FOR SELECT TO public USING (private.current_actor_type() = 'admin'::public.user_type);

CREATE INDEX IF NOT EXISTS task_amount_settings_provider_type_idx
  ON public.task_amount_settings(provider_id, task_type, is_active);

-- The old schema had no task amount source. Do not preserve package.price as an
-- operational task amount; existing rows are explicitly marked as zero until
-- an administrator records the company/type amount.
UPDATE public.payment_tasks SET amount_snapshot = 0 WHERE amount_snapshot IS NOT NULL;

CREATE OR REPLACE FUNCTION public.admin_upsert_task_amount_setting(
  p_id uuid,
  p_provider_id uuid,
  p_task_type text,
  p_amount numeric,
  p_currency text,
  p_is_active boolean
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_id uuid;
BEGIN
  IF NOT private.has_permission('settings.manage') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.telecom_providers WHERE id=p_provider_id) THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;
  IF nullif(trim(p_task_type), '') IS NULL OR p_amount IS NULL OR p_amount < 0 THEN RAISE EXCEPTION 'TASK_AMOUNT_INVALID'; END IF;
  IF nullif(trim(p_currency), '') IS NULL THEN RAISE EXCEPTION 'TASK_AMOUNT_CURRENCY_REQUIRED'; END IF;

  IF p_id IS NULL THEN
    INSERT INTO public.task_amount_settings(provider_id, task_type, amount, currency, is_active)
    VALUES (p_provider_id, trim(p_task_type), p_amount, trim(p_currency), coalesce(p_is_active,true))
    ON CONFLICT (provider_id, task_type) DO UPDATE
      SET amount=excluded.amount, currency=excluded.currency, is_active=excluded.is_active, updated_at=now()
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.task_amount_settings
    SET provider_id=p_provider_id, task_type=trim(p_task_type), amount=p_amount,
        currency=trim(p_currency), is_active=coalesce(p_is_active,true), updated_at=now()
    WHERE id=p_id RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'TASK_AMOUNT_SETTING_NOT_FOUND'; END IF;
  END IF;

  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'upsert_task_settings', 'task_amount_setting', v_id,
          jsonb_build_object('provider_id',p_provider_id,'task_type',p_task_type,'amount',p_amount,'currency',p_currency));
  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.approve_protection_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_req record; v_pkg record; v_number_provider uuid; v_prot uuid; v_amount numeric; v_currency text;
BEGIN
  IF NOT private.has_permission('requests.approve') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  SELECT * INTO v_req FROM public.protection_requests WHERE id=p_request_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_req.request_type <> 'new' THEN RAISE EXCEPTION 'USE_RENEWAL_APPROVAL'; END IF;
  IF v_req.status <> 'under_review' OR v_req.payment_verified_at IS NULL THEN RAISE EXCEPTION 'PAYMENT_NOT_VERIFIED'; END IF;
  SELECT p.* INTO v_pkg FROM public.packages p WHERE p.id=v_req.package_id AND p.is_active;
  IF NOT FOUND THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  SELECT pn.provider_id INTO v_number_provider FROM public.customer_numbers cn JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id WHERE cn.id=v_req.customer_number_id FOR UPDATE;
  IF v_number_provider IS NULL OR v_number_provider <> v_pkg.provider_id THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;
  IF EXISTS (SELECT 1 FROM public.protections WHERE customer_number_id=v_req.customer_number_id AND status='active'::public.protection_status) THEN RAISE EXCEPTION 'ACTIVE_PROTECTION_EXISTS'; END IF;

  INSERT INTO public.protections(customer_number_id,package_id,request_id,starts_at,expires_at,status,package_price_snapshot,package_duration_days_snapshot,package_currency_snapshot)
  VALUES (v_req.customer_number_id,v_req.package_id,v_req.id,now(),now()+(v_req.duration_days_snapshot||' days')::interval,'active'::public.protection_status,v_req.price_snapshot,v_req.duration_days_snapshot,v_req.currency_snapshot)
  RETURNING id INTO v_prot;
  UPDATE public.protection_requests SET status='approved',reviewed_at=now(),reviewed_by=auth.uid(),updated_at=now() WHERE id=p_request_id;

  SELECT amount, currency INTO v_amount, v_currency
  FROM public.task_amount_settings
  WHERE provider_id=v_number_provider AND task_type='initial_activation' AND is_active
  LIMIT 1;
  IF coalesce((SELECT first_task_enabled FROM public.task_settings WHERE provider_id=v_number_provider AND is_active LIMIT 1),true) THEN
    INSERT INTO public.payment_tasks(protection_id,provider_id,task_type,due_date,telecom_due_at,visible_from,planned_due_at,status,amount_snapshot,amount_currency)
    VALUES(v_prot,v_number_provider,'initial_activation',now(),now(),now(),now(),'open'::public.task_status,coalesce(v_amount,0),coalesce(v_currency,'YER'));
  END IF;

  INSERT INTO public.financial_transactions(type,amount,currency,reference_id,notes,created_by)
  VALUES('customer_payment',v_req.price_snapshot,v_req.currency_snapshot,v_prot,'رسوم حماية معتمدة',auth.uid());
  INSERT INTO public.client_notifications(customer_id,title,message,type,related_entity_type,related_entity_id)
  VALUES(v_req.customer_id,'تم تفعيل الحماية','تمت الموافقة على طلب الحماية وبدأت فترة الحماية بنجاح','protection_started','protection',v_prot);
  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'approve_protection_request','protection',v_prot,jsonb_build_object('request_id',p_request_id,'price_snapshot',v_req.price_snapshot,'duration_days_snapshot',v_req.duration_days_snapshot));
  UPDATE public.protection_requests SET status='rejected', rejection_reason='CONFLICTING_PROTECTION_ACCEPTED', reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now()
  WHERE customer_number_id=v_req.customer_number_id AND id<>p_request_id AND status IN ('pending_payment_verification','under_review');
  RETURN v_prot;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.admin_upsert_task_amount_setting(uuid,uuid,text,numeric,text,boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_upsert_task_amount_setting(uuid,uuid,text,numeric,text,boolean) TO authenticated;

COMMIT;
