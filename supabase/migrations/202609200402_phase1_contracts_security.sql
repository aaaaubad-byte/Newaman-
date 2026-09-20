-- AMAN Phase 1: database contracts, historical snapshots, task model, and security boundaries.
-- This migration is intentionally additive and keeps all historical rows.

BEGIN;

-- 1. Historical snapshots and request metadata.
ALTER TABLE public.packages
  ADD COLUMN IF NOT EXISTS description text,
  ADD COLUMN IF NOT EXISTS is_visible_to_customer boolean NOT NULL DEFAULT true;

ALTER TABLE public.protection_requests
  ADD COLUMN IF NOT EXISTS request_type text NOT NULL DEFAULT 'new',
  ADD COLUMN IF NOT EXISTS previous_protection_id uuid,
  ADD COLUMN IF NOT EXISTS price_snapshot numeric,
  ADD COLUMN IF NOT EXISTS duration_days_snapshot integer,
  ADD COLUMN IF NOT EXISTS currency_snapshot text;

ALTER TABLE public.protections
  ADD COLUMN IF NOT EXISTS package_price_snapshot numeric,
  ADD COLUMN IF NOT EXISTS package_duration_days_snapshot integer,
  ADD COLUMN IF NOT EXISTS package_currency_snapshot text;

UPDATE public.protection_requests r
SET price_snapshot = p.price,
    duration_days_snapshot = p.duration_days,
    currency_snapshot = p.currency
FROM public.packages p
WHERE p.id = r.package_id
  AND (r.price_snapshot IS NULL OR r.duration_days_snapshot IS NULL OR r.currency_snapshot IS NULL);

UPDATE public.protections x
SET package_price_snapshot = p.price,
    package_duration_days_snapshot = p.duration_days,
    package_currency_snapshot = p.currency
FROM public.packages p
WHERE p.id = x.package_id
  AND (x.package_price_snapshot IS NULL OR x.package_duration_days_snapshot IS NULL OR x.package_currency_snapshot IS NULL);

ALTER TABLE public.protection_requests
  ALTER COLUMN price_snapshot SET DEFAULT 0,
  ALTER COLUMN duration_days_snapshot SET DEFAULT 0,
  ALTER COLUMN currency_snapshot SET DEFAULT 'YER';

ALTER TABLE public.protections
  ALTER COLUMN package_price_snapshot SET DEFAULT 0,
  ALTER COLUMN package_duration_days_snapshot SET DEFAULT 0,
  ALTER COLUMN package_currency_snapshot SET DEFAULT 'YER';

ALTER TABLE public.protection_requests
  ADD CONSTRAINT protection_requests_request_type_ck
    CHECK (request_type IN ('new', 'renewal')),
  ADD CONSTRAINT protection_requests_snapshot_values_ck
    CHECK (price_snapshot >= 0 AND duration_days_snapshot > 0 AND nullif(trim(currency_snapshot), '') IS NOT NULL);

ALTER TABLE public.protections
  ADD CONSTRAINT protections_snapshot_values_ck
    CHECK (package_price_snapshot >= 0 AND package_duration_days_snapshot > 0 AND nullif(trim(package_currency_snapshot), '') IS NOT NULL);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'protection_requests_previous_protection_fk'
  ) THEN
    ALTER TABLE public.protection_requests
      ADD CONSTRAINT protection_requests_previous_protection_fk
      FOREIGN KEY (previous_protection_id) REFERENCES public.protections(id);
  END IF;
END $$;

ALTER TABLE public.payment_tasks
  ADD COLUMN IF NOT EXISTS amount_snapshot numeric,
  ADD COLUMN IF NOT EXISTS amount_currency text;

UPDATE public.payment_tasks t
SET amount_snapshot = x.package_price_snapshot,
    amount_currency = x.package_currency_snapshot
FROM public.protections x
WHERE x.id = t.protection_id
  AND (t.amount_snapshot IS NULL OR t.amount_currency IS NULL);

ALTER TABLE public.payment_tasks
  ALTER COLUMN amount_snapshot SET DEFAULT 0,
  ALTER COLUMN amount_currency SET DEFAULT 'YER';

ALTER TABLE public.payment_tasks
  ADD CONSTRAINT payment_tasks_amount_snapshot_ck
    CHECK (amount_snapshot >= 0 AND nullif(trim(amount_currency), '') IS NOT NULL);

-- 2. Task statuses: retain the legacy enum value temporarily for old clients,
-- but all new server writes use open/completed/cancelled. The enum values are
-- added by the committed prerequisite migration immediately before this one.
UPDATE public.payment_tasks SET status = 'open'::public.task_status WHERE status = 'due'::public.task_status;
ALTER TABLE public.payment_tasks ALTER COLUMN status SET DEFAULT 'open'::public.task_status;

-- 3. Settings tables already referenced by Android.
CREATE TABLE IF NOT EXISTS public.task_time_classifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  task_settings_id uuid NOT NULL REFERENCES public.task_settings(id),
  name text NOT NULL,
  min_days_remaining integer,
  max_days_remaining integer,
  sort_order integer NOT NULL DEFAULT 0,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT task_time_classifications_name_ck CHECK (nullif(trim(name), '') IS NOT NULL),
  CONSTRAINT task_time_classifications_range_ck CHECK (
    min_days_remaining IS NULL OR max_days_remaining IS NULL OR min_days_remaining <= max_days_remaining
  )
);

CREATE TABLE IF NOT EXISTS public.notification_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  type text NOT NULL,
  recipient public.notification_recipient NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  days_before integer,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT notification_settings_type_ck CHECK (nullif(trim(type), '') IS NOT NULL),
  CONSTRAINT notification_settings_days_ck CHECK (days_before IS NULL OR days_before >= 0),
  CONSTRAINT notification_settings_unique_ck UNIQUE (type, recipient)
);

CREATE INDEX IF NOT EXISTS task_time_classifications_settings_idx
  ON public.task_time_classifications(task_settings_id, is_active, sort_order);
CREATE INDEX IF NOT EXISTS payment_tasks_protection_status_idx
  ON public.payment_tasks(protection_id, status, due_date);
CREATE INDEX IF NOT EXISTS protections_active_number_idx
  ON public.protections(customer_number_id)
  WHERE status = 'active'::public.protection_status;
CREATE INDEX IF NOT EXISTS protection_requests_previous_protection_idx
  ON public.protection_requests(previous_protection_id)
  WHERE previous_protection_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS protections_one_active_per_number_uidx
  ON public.protections(customer_number_id)
  WHERE status = 'active'::public.protection_status;

-- 4. RLS for the newly materialized settings tables.
ALTER TABLE public.task_time_classifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS task_classifications_admin_read ON public.task_time_classifications;
CREATE POLICY task_classifications_admin_read ON public.task_time_classifications
  FOR SELECT TO public USING (private.current_actor_type() = 'admin'::public.user_type);

DROP POLICY IF EXISTS notification_settings_admin_read ON public.notification_settings;
CREATE POLICY notification_settings_admin_read ON public.notification_settings
  FOR SELECT TO public USING (private.current_actor_type() = 'admin'::public.user_type);

-- 5. Server-side RPCs for settings management.
CREATE OR REPLACE FUNCTION public.admin_upsert_task_classification(
  p_id uuid,
  p_task_settings_id uuid,
  p_name text,
  p_min_days integer,
  p_max_days integer,
  p_sort_order integer,
  p_is_active boolean
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_id uuid;
BEGIN
  IF NOT private.has_permission('settings.manage') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.task_settings WHERE id = p_task_settings_id) THEN RAISE EXCEPTION 'TASK_SETTINGS_NOT_FOUND'; END IF;
  IF nullif(trim(p_name), '') IS NULL THEN RAISE EXCEPTION 'CLASSIFICATION_NAME_REQUIRED'; END IF;
  IF p_min_days IS NOT NULL AND p_max_days IS NOT NULL AND p_min_days > p_max_days THEN RAISE EXCEPTION 'CLASSIFICATION_RANGE_INVALID'; END IF;

  IF p_id IS NULL THEN
    INSERT INTO public.task_time_classifications(task_settings_id, name, min_days_remaining, max_days_remaining, sort_order, is_active)
    VALUES (p_task_settings_id, trim(p_name), p_min_days, p_max_days, coalesce(p_sort_order, 0), coalesce(p_is_active, true))
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.task_time_classifications
    SET task_settings_id = p_task_settings_id,
        name = trim(p_name),
        min_days_remaining = p_min_days,
        max_days_remaining = p_max_days,
        sort_order = coalesce(p_sort_order, 0),
        is_active = coalesce(p_is_active, true),
        updated_at = now()
    WHERE id = p_id
    RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'CLASSIFICATION_NOT_FOUND'; END IF;
  END IF;

  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'upsert_task_classification', 'task_time_classification', v_id,
          jsonb_build_object('task_settings_id', p_task_settings_id, 'name', trim(p_name)));
  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_upsert_notification_setting(
  p_id uuid,
  p_type text,
  p_recipient text,
  p_enabled boolean,
  p_days_before integer
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_id uuid; v_recipient public.notification_recipient;
BEGIN
  IF NOT private.has_permission('settings.manage') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF nullif(trim(p_type), '') IS NULL THEN RAISE EXCEPTION 'NOTIFICATION_TYPE_REQUIRED'; END IF;
  IF p_days_before IS NOT NULL AND p_days_before < 0 THEN RAISE EXCEPTION 'NOTIFICATION_DAYS_INVALID'; END IF;
  BEGIN
    v_recipient := lower(trim(p_recipient))::public.notification_recipient;
  EXCEPTION WHEN invalid_text_representation THEN
    RAISE EXCEPTION 'NOTIFICATION_RECIPIENT_INVALID';
  END;

  IF p_id IS NULL THEN
    INSERT INTO public.notification_settings(type, recipient, enabled, days_before)
    VALUES (trim(p_type), v_recipient, coalesce(p_enabled, true), p_days_before)
    ON CONFLICT (type, recipient) DO UPDATE
      SET enabled = excluded.enabled, days_before = excluded.days_before, updated_at = now()
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.notification_settings
    SET type = trim(p_type), recipient = v_recipient, enabled = coalesce(p_enabled, true),
        days_before = p_days_before, updated_at = now()
    WHERE id = p_id
    RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'NOTIFICATION_SETTING_NOT_FOUND'; END IF;
  END IF;

  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'upsert_notification_setting', 'notification_setting', v_id,
          jsonb_build_object('type', trim(p_type), 'recipient', v_recipient, 'enabled', p_enabled));
  RETURN v_id;
END;
$$;

-- 6. Request submission, payment verification, rejection, and approval guards.
CREATE OR REPLACE FUNCTION public.submit_protection_request(
  p_number_id uuid, p_package_id uuid, p_wallet_id uuid, p_transfer_ref text, p_idempotency_key text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_request_id uuid; v_existing uuid; v_number_provider uuid; v_package_provider uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'UNAUTHENTICATED'; END IF;
  SELECT pn.provider_id INTO v_number_provider
  FROM public.customer_numbers cn JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE cn.id = p_number_id AND cn.customer_id = auth.uid() AND cn.is_active;
  IF v_number_provider IS NULL THEN RAISE EXCEPTION 'NUMBER_NOT_FOUND'; END IF;
  SELECT provider_id INTO v_package_provider FROM public.packages WHERE id = p_package_id AND is_active AND is_visible_to_customer;
  IF v_package_provider IS NULL THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_number_provider <> v_package_provider THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.payment_methods WHERE id = p_wallet_id AND is_active) THEN RAISE EXCEPTION 'PAYMENT_METHOD_NOT_FOUND'; END IF;
  IF nullif(trim(p_transfer_ref), '') IS NULL THEN RAISE EXCEPTION 'TRANSFER_REFERENCE_REQUIRED'; END IF;

  IF p_idempotency_key IS NOT NULL THEN
    SELECT id INTO v_existing FROM public.protection_requests WHERE idempotency_key = p_idempotency_key;
    IF v_existing IS NOT NULL THEN RETURN v_existing; END IF;
  END IF;

  INSERT INTO public.protection_requests(
    customer_id, customer_number_id, package_id, payment_method_id, transfer_reference,
    idempotency_key, request_type, price_snapshot, duration_days_snapshot, currency_snapshot, status
  )
  SELECT auth.uid(), p_number_id, p_package_id, p_wallet_id, trim(p_transfer_ref), p_idempotency_key,
         'new', p.price, p.duration_days, p.currency, 'pending_payment_verification'::public.request_status
  FROM public.packages p WHERE p.id = p_package_id
  RETURNING id INTO v_request_id;

  INSERT INTO public.admin_notifications(title, message, type, related_entity_type, related_entity_id)
  VALUES ('طلب حماية جديد', 'تم تقديم طلب حماية جديد يتطلب مراجعة الدفع', 'request_submitted', 'protection_request', v_request_id);
  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id)
  VALUES (auth.uid(), 'submit_protection_request', 'protection_request', v_request_id);
  RETURN v_request_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.submit_renewal_request(
  p_protection_id uuid, p_package_id uuid, p_wallet_id uuid, p_transfer_ref text
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_number_id uuid; v_customer_id uuid; v_provider_id uuid; v_package_provider uuid; v_req_id uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'UNAUTHENTICATED'; END IF;
  SELECT p.customer_number_id, cn.customer_id, pn.provider_id
    INTO v_number_id, v_customer_id, v_provider_id
  FROM public.protections p
  JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE p.id = p_protection_id AND cn.customer_id = auth.uid() AND p.status = 'active'::public.protection_status;
  IF v_number_id IS NULL THEN RAISE EXCEPTION 'PROTECTION_NOT_FOUND'; END IF;
  SELECT provider_id INTO v_package_provider FROM public.packages WHERE id = p_package_id AND is_active AND is_visible_to_customer;
  IF v_package_provider IS NULL THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_provider_id <> v_package_provider THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.payment_methods WHERE id = p_wallet_id AND is_active) THEN RAISE EXCEPTION 'PAYMENT_METHOD_NOT_FOUND'; END IF;
  IF nullif(trim(p_transfer_ref), '') IS NULL THEN RAISE EXCEPTION 'TRANSFER_REFERENCE_REQUIRED'; END IF;
  IF EXISTS (SELECT 1 FROM public.protection_requests WHERE previous_protection_id = p_protection_id AND status IN ('pending_payment_verification','under_review')) THEN
    RAISE EXCEPTION 'RENEWAL_ALREADY_PENDING';
  END IF;

  INSERT INTO public.protection_requests(
    customer_id, customer_number_id, package_id, payment_method_id, transfer_reference,
    request_type, previous_protection_id, price_snapshot, duration_days_snapshot, currency_snapshot, status
  )
  SELECT v_customer_id, v_number_id, p_package_id, p_wallet_id, trim(p_transfer_ref),
         'renewal', p_protection_id, p.price, p.duration_days, p.currency, 'pending_payment_verification'::public.request_status
  FROM public.packages p WHERE p.id = p_package_id
  RETURNING id INTO v_req_id;

  INSERT INTO public.admin_notifications(title, message, type, related_entity_type, related_entity_id)
  VALUES ('طلب تجديد جديد', 'تم تقديم طلب تجديد مرتبط بحماية سارية', 'renewal_submitted', 'protection_request', v_req_id);
  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id)
  VALUES (auth.uid(), 'submit_renewal_request', 'protection_request', v_req_id);
  RETURN v_req_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.verify_request_payment(
  p_request_id uuid, p_verified boolean, p_note text DEFAULT NULL
) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_status public.request_status;
BEGIN
  IF NOT private.has_permission('finance.verify') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  SELECT status INTO v_status FROM public.protection_requests WHERE id = p_request_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_status NOT IN ('pending_payment_verification','under_review') THEN RAISE EXCEPTION 'INVALID_REQUEST_STATUS'; END IF;
  IF p_verified THEN
    UPDATE public.protection_requests SET status='under_review', payment_verified_at=now(), payment_verified_by=auth.uid(), verified_note=p_note, updated_at=now() WHERE id=p_request_id;
  ELSE
    UPDATE public.protection_requests SET status='rejected', rejection_reason=coalesce(nullif(trim(p_note), ''), 'TRANSFER_NOT_RECEIVED'), payment_verified_by=auth.uid(), verified_note=p_note, reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now() WHERE id=p_request_id;
  END IF;
  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'verify_payment', 'protection_request', p_request_id, jsonb_build_object('verified', p_verified, 'note', p_note));
  RETURN true;
END;
$$;

CREATE OR REPLACE FUNCTION public.reject_protection_request(
  p_request_id uuid, p_rejection_reason text
) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_req record; v_reason text;
BEGIN
  IF NOT (private.has_permission('requests.reject') OR private.has_permission('finance.verify')) THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  v_reason := nullif(trim(p_rejection_reason), '');
  IF v_reason IS NULL THEN RAISE EXCEPTION 'REJECTION_REASON_REQUIRED'; END IF;
  SELECT * INTO v_req FROM public.protection_requests WHERE id=p_request_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_req.status NOT IN ('pending_payment_verification','under_review') THEN RAISE EXCEPTION 'INVALID_REQUEST_STATUS'; END IF;
  UPDATE public.protection_requests
  SET status='rejected', rejection_reason=v_reason, reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now()
  WHERE id=p_request_id;
  INSERT INTO public.client_notifications(customer_id,title,message,type,related_entity_type,related_entity_id)
  VALUES (v_req.customer_id,'تم رفض الطلب',v_reason,'request_rejected','protection_request',p_request_id);
  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES (auth.uid(),'reject_protection_request','protection_request',p_request_id,jsonb_build_object('reason',v_reason));
  RETURN true;
END;
$$;

-- 7. Replace approval with a guarded atomic path. Renewal approval is added in phase 2,
-- while this phase makes new-protection approval safe and snapshot-based.
CREATE OR REPLACE FUNCTION public.approve_protection_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_req record; v_pkg record; v_number_provider uuid; v_prot uuid; v_task_amount numeric; v_task_currency text;
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
  -- Task amount is independent from package price. Phase 3 will add per-task-type settings;
  -- use zero until a company task amount is configured rather than copying packages.price.
  v_task_amount := 0;
  v_task_currency := v_req.currency_snapshot;
  IF coalesce((SELECT first_task_enabled FROM public.task_settings WHERE provider_id=v_number_provider AND is_active LIMIT 1),true) THEN
    INSERT INTO public.payment_tasks(protection_id,provider_id,task_type,due_date,telecom_due_at,visible_from,planned_due_at,status,amount_snapshot,amount_currency)
    VALUES(v_prot,v_number_provider,'initial_activation',now(),now(),now(),now(),'open'::public.task_status,v_task_amount,v_task_currency);
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

-- 8. Task completion is idempotent and writes open tasks only.
CREATE OR REPLACE FUNCTION public.complete_payment_task(p_task_id uuid, p_telecom_ref text DEFAULT NULL)
RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_task record; v_provider uuid; v_interval integer; v_visibility integer; v_next timestamptz;
BEGIN
  IF NOT private.has_permission('tasks.complete') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  SELECT * INTO v_task FROM public.payment_tasks WHERE id=p_task_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'TASK_NOT_FOUND'; END IF;
  IF v_task.status='completed'::public.task_status THEN RETURN true; END IF;
  IF v_task.status='cancelled'::public.task_status THEN RAISE EXCEPTION 'TASK_CANCELLED'; END IF;
  UPDATE public.payment_tasks SET status='completed',completed_at=now(),completed_by=auth.uid(),telecom_reference=nullif(trim(coalesce(p_telecom_ref,'')),''),updated_at=now() WHERE id=p_task_id;
  SELECT coalesce(v_task.provider_id,pn.provider_id) INTO v_provider
  FROM public.protections p JOIN public.customer_numbers cn ON cn.id=p.customer_number_id JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id
  WHERE p.id=v_task.protection_id;
  IF v_provider IS NULL THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;
  SELECT coalesce(default_interval_days,90),coalesce(visibility_days_before,30) INTO v_interval,v_visibility FROM public.task_settings WHERE provider_id=v_provider AND is_active LIMIT 1;
  v_interval:=coalesce(v_interval,90); v_visibility:=coalesce(v_visibility,30); v_next:=now()+(v_interval||' days')::interval;
  IF EXISTS (SELECT 1 FROM public.protections WHERE id=v_task.protection_id AND status='active'::public.protection_status AND expires_at>v_next)
     AND NOT EXISTS (SELECT 1 FROM public.payment_tasks WHERE protection_id=v_task.protection_id AND task_type='recurring' AND status IN ('open'::public.task_status,'completed'::public.task_status) AND planned_due_at=v_next) THEN
    INSERT INTO public.payment_tasks(protection_id,provider_id,task_type,due_date,telecom_due_at,planned_due_at,visible_from,status,amount_snapshot,amount_currency)
    SELECT v_task.protection_id,v_provider,'recurring',v_next,v_next,v_next,v_next-(v_visibility||' days')::interval,'open'::public.task_status,coalesce(v_task.amount_snapshot,0),coalesce(v_task.amount_currency,'YER');
  END IF;
  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'complete_payment_task','payment_task',p_task_id,jsonb_build_object('next_due_at',v_next));
  RETURN true;
END;
$$;

-- 9. Views expose historical snapshots and no longer hide completed/cancelled tasks.
DROP VIEW IF EXISTS public.admin_payment_tasks;
CREATE VIEW public.admin_payment_tasks WITH (security_invoker=true) AS
SELECT pt.id, pt.protection_id, p.customer_number_id,
       coalesce(pt.provider_id,pn.provider_id) AS provider_id,
       u.full_name AS customer_name, pn.number, tp.name_ar AS provider_name,
       pt.task_type, pt.due_date AS due_at, coalesce(pt.telecom_due_at,pt.due_date) AS telecom_due_at,
       ceil(extract(epoch FROM (coalesce(pt.telecom_due_at,pt.due_date)-now()))/86400.0)::integer AS days_remaining,
       CASE WHEN pt.status='open'::public.task_status AND coalesce(pt.telecom_due_at,pt.due_date)<now() THEN 'overdue' ELSE pt.status::text END AS calculated_status,
       pt.status, pt.completed_at, pt.completed_by, pt.telecom_reference, pt.rescheduled_reason,
       pt.scheduled_at, pt.created_at, pt.updated_at, pt.amount_snapshot, pt.amount_currency,
       pt.cycle_number, pt.visible_from, pt.planned_due_at,
       EXISTS (SELECT 1 FROM public.task_time_classifications c JOIN public.task_settings ts ON ts.id=c.task_settings_id
               WHERE ts.provider_id=coalesce(pt.provider_id,pn.provider_id) AND c.is_active
                 AND (c.min_days_remaining IS NULL OR ceil(extract(epoch FROM (coalesce(pt.telecom_due_at,pt.due_date)-now()))/86400.0)::integer >= c.min_days_remaining)
                 AND (c.max_days_remaining IS NULL OR ceil(extract(epoch FROM (coalesce(pt.telecom_due_at,pt.due_date)-now()))/86400.0)::integer <= c.max_days_remaining)) AS has_classification
FROM public.payment_tasks pt
JOIN public.protections p ON p.id=pt.protection_id
JOIN public.customer_numbers cn ON cn.id=p.customer_number_id
JOIN public.users u ON u.id=cn.customer_id
JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id
JOIN public.telecom_providers tp ON tp.id=coalesce(pt.provider_id,pn.provider_id);

DROP VIEW IF EXISTS public.admin_all_protections;
CREATE VIEW public.admin_all_protections WITH (security_invoker=true) AS
SELECT p.id,p.request_id,cn.customer_id,p.customer_number_id,u.full_name AS customer_name,u.phone AS customer_phone,
       pn.number,pn.provider_id,tp.name_ar AS provider_name,
       p.package_price_snapshot,p.package_duration_days_snapshot,p.package_currency_snapshot,
       p.starts_at,p.expires_at,p.status,p.created_at
FROM public.protections p
JOIN public.customer_numbers cn ON cn.id=p.customer_number_id
JOIN public.users u ON u.id=cn.customer_id
JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id
JOIN public.telecom_providers tp ON tp.id=pn.provider_id;

-- 10. Explicit grants. RLS and private.has_permission remain the authorization boundary.
GRANT SELECT ON public.task_time_classifications, public.notification_settings TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_task_classification(uuid,uuid,text,integer,integer,integer,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_notification_setting(uuid,text,text,boolean,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_renewal_request(uuid,uuid,uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.verify_request_payment(uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_protection_request(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_protection_request(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_payment_task(uuid,text) TO authenticated;

COMMIT;
