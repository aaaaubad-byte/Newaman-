-- AMAN Phase P0: Core Repair & Contract Enforcement
-- Reconciles all live database issues: renewal approval, duplication guard, task lifecycle, and RLS security.

BEGIN;

-- 1. Strict Duplication Prevention: Partial Unique Index
-- Guarantee that at the DB level, no two unresolved requests exist for the same customer number.
CREATE UNIQUE INDEX IF NOT EXISTS protection_requests_unresolved_number_uidx
  ON public.protection_requests(customer_number_id)
  WHERE status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status);

-- 2. Enhanced submit_protection_request with comprehensive duplication & concurrency guards
CREATE OR REPLACE FUNCTION public.submit_protection_request(
  p_number_id uuid,
  p_package_id uuid,
  p_wallet_id uuid,
  p_transfer_ref text,
  p_idempotency_key text DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_num record;
  v_pkg record;
  v_wal record;
  v_req_id uuid;
  v_idem text;
  v_ref text;
  v_existing_id uuid;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'NOT_AUTHENTICATED';
  END IF;

  v_idem := nullif(trim(coalesce(p_idempotency_key, '')), '');
  v_ref  := nullif(trim(coalesce(p_transfer_ref, '')), '');

  IF v_ref IS NULL THEN
    RAISE EXCEPTION 'TRANSFER_REFERENCE_REQUIRED';
  END IF;

  -- Verify and lock the customer number
  SELECT cn.id, cn.customer_id, cn.phone_number_id, pn.number, pn.provider_id
  INTO v_num
  FROM public.customer_numbers cn
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE cn.id = p_number_id AND cn.customer_id = auth.uid() AND cn.is_active = true
  FOR SHARE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'CUSTOMER_NUMBER_NOT_FOUND';
  END IF;

  -- Idempotency check: return existing request if key already used by this customer
  IF v_idem IS NOT NULL THEN
    SELECT id INTO v_existing_id
    FROM public.protection_requests
    WHERE idempotency_key = v_idem AND customer_id = auth.uid();

    IF v_existing_id IS NOT NULL THEN
      RETURN v_existing_id;
    END IF;
  END IF;

  -- Check if an active protection already exists for this number
  IF EXISTS (
    SELECT 1 FROM public.protections p
    JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
    WHERE cn.phone_number_id = v_num.phone_number_id
      AND p.status = 'active'::public.protection_status
  ) THEN
    RAISE EXCEPTION 'NUMBER_ALREADY_PROTECTED';
  END IF;

  -- Check if an unresolved request already exists for this number
  IF EXISTS (
    SELECT 1 FROM public.protection_requests pr
    JOIN public.customer_numbers cn ON cn.id = pr.customer_number_id
    WHERE cn.phone_number_id = v_num.phone_number_id
      AND pr.status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status)
  ) THEN
    RAISE EXCEPTION 'CONFLICTING_REQUEST_EXISTS';
  END IF;

  -- Verify package validity, active status, customer visibility, and provider match
  SELECT id, provider_id, duration_days, price, currency, is_active, is_visible_to_customer
  INTO v_pkg
  FROM public.packages
  WHERE id = p_package_id;

  IF NOT FOUND OR NOT v_pkg.is_active OR NOT v_pkg.is_visible_to_customer THEN
    RAISE EXCEPTION 'INVALID_PACKAGE';
  END IF;

  IF v_pkg.provider_id <> v_num.provider_id THEN
    RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH';
  END IF;

  -- Verify active payment method
  SELECT id, is_active INTO v_wal
  FROM public.payment_methods
  WHERE id = p_wallet_id AND is_active = true;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'INVALID_PAYMENT_METHOD';
  END IF;

  -- Insert the request with all required snapshots and pending status
  INSERT INTO public.protection_requests (
    customer_id,
    customer_number_id,
    package_id,
    payment_method_id,
    payment_reference,
    idempotency_key,
    price_snapshot,
    duration_days_snapshot,
    currency_snapshot,
    status,
    request_type,
    created_at,
    updated_at
  ) VALUES (
    auth.uid(),
    p_number_id,
    p_package_id,
    p_wallet_id,
    v_ref,
    v_idem,
    v_pkg.price,
    v_pkg.duration_days,
    coalesce(v_pkg.currency, 'YER'),
    'pending_payment_verification'::public.request_status,
    'new'::public.request_type,
    now(),
    now()
  ) RETURNING id INTO v_req_id;

  -- Insert admin notification
  INSERT INTO public.admin_notifications (
    type,
    title,
    message,
    reference_id,
    created_at
  ) VALUES (
    'new_protection_request',
    'طلب حماية جديد',
    'تم استلام طلب حماية جديد للرقم ' || v_num.number,
    v_req_id,
    now()
  );

  -- Insert audit log
  INSERT INTO public.audit_logs (
    actor_id,
    action,
    entity_type,
    entity_id,
    after_data,
    created_at
  ) VALUES (
    auth.uid(),
    'submit_protection_request',
    'protection_request',
    v_req_id,
    jsonb_build_object(
      'number_id', p_number_id,
      'package_id', p_package_id,
      'price_snapshot', v_pkg.price,
      'duration_days_snapshot', v_pkg.duration_days
    ),
    now()
  );

  RETURN v_req_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) TO authenticated;


-- 3. Correct approve_renewal_request implementation
CREATE OR REPLACE FUNCTION public.approve_renewal_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_req record;
  v_current record;
  v_pkg record;
  v_new_expiry timestamptz;
BEGIN
  IF NOT private.has_permission('requests.approve') THEN
    RAISE EXCEPTION 'PERMISSION_DENIED';
  END IF;

  -- Lock and validate the renewal request
  SELECT * INTO v_req
  FROM public.protection_requests
  WHERE id = p_request_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'REQUEST_NOT_FOUND';
  END IF;

  IF v_req.status = 'approved'::public.request_status THEN
    RETURN v_req.previous_protection_id;
  END IF;

  IF v_req.request_type <> 'renewal'::public.request_type OR v_req.previous_protection_id IS NULL THEN
    RAISE EXCEPTION 'INVALID_RENEWAL_REQUEST';
  END IF;

  IF v_req.status <> 'under_review'::public.request_status OR v_req.payment_verified_at IS NULL THEN
    RAISE EXCEPTION 'PAYMENT_NOT_VERIFIED';
  END IF;

  -- Lock and validate existing active protection
  SELECT * INTO v_current
  FROM public.protections
  WHERE id = v_req.previous_protection_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'PROTECTION_NOT_FOUND';
  END IF;

  IF v_current.status <> 'active'::public.protection_status THEN
    RAISE EXCEPTION 'PROTECTION_NOT_ACTIVE';
  END IF;

  IF v_current.customer_id <> v_req.customer_id OR v_current.customer_number_id <> v_req.customer_number_id THEN
    RAISE EXCEPTION 'PROTECTION_MISMATCH';
  END IF;

  -- Validate package
  SELECT id, provider_id, duration_days, price, currency, is_active
  INTO v_pkg
  FROM public.packages
  WHERE id = v_req.package_id;

  IF NOT FOUND OR NOT v_pkg.is_active THEN
    RAISE EXCEPTION 'INVALID_PACKAGE';
  END IF;

  -- Calculate new expiry extending from current active protection expiry
  v_new_expiry := v_current.expires_at + (v_req.duration_days_snapshot || ' days')::interval;

  -- Atomically extend the existing protection
  UPDATE public.protections
  SET
    package_id = v_req.package_id,
    expires_at = v_new_expiry,
    package_price_snapshot = v_req.price_snapshot,
    package_duration_days_snapshot = v_req.duration_days_snapshot,
    package_currency_snapshot = coalesce(v_req.currency_snapshot, 'YER'),
    status = 'active'::public.protection_status,
    updated_at = now()
  WHERE id = v_current.id;

  -- Mark the request approved
  UPDATE public.protection_requests
  SET
    status = 'approved'::public.request_status,
    reviewed_at = now(),
    reviewed_by = auth.uid(),
    updated_at = now()
  WHERE id = v_req.id;

  -- Record protection history
  INSERT INTO public.protection_history (
    protection_id,
    event,
    notes,
    created_at
  ) VALUES (
    v_current.id,
    'renewal_approved',
    'تم اعتماد تجديد الحماية بنجاح وتمديد فترة الصلاحية إلى ' || to_char(v_new_expiry, 'YYYY-MM-DD'),
    now()
  );

  -- Record financial transaction
  INSERT INTO public.financial_transactions (
    type,
    amount,
    currency,
    reference_id,
    notes,
    created_by,
    created_at
  ) VALUES (
    'customer_payment'::public.transaction_type,
    coalesce(v_req.price_snapshot, 0),
    coalesce(v_req.currency_snapshot, 'YER'),
    v_current.id,
    'دفعة تجديد الحماية',
    auth.uid(),
    now()
  );

  -- Notify customer
  INSERT INTO public.client_notifications (
    customer_id,
    type,
    title,
    message,
    reference_id,
    created_at
  ) VALUES (
    v_req.customer_id,
    'renewal_approved',
    'تم تجديد الحماية',
    'تم اعتماد طلب تجديد الحماية لرقمك حتى ' || to_char(v_new_expiry, 'YYYY-MM-DD'),
    v_current.id,
    now()
  );

  -- Log audit
  INSERT INTO public.audit_logs (
    actor_id,
    action,
    entity_type,
    entity_id,
    after_data,
    created_at
  ) VALUES (
    auth.uid(),
    'approve_renewal_request',
    'protection_request',
    v_req.id,
    jsonb_build_object(
      'protection_id', v_current.id,
      'new_expires_at', v_new_expiry,
      'amount', v_req.price_snapshot
    ),
    now()
  );

  -- Close any other pending renewal requests for this protection
  UPDATE public.protection_requests
  SET
    status = 'rejected'::public.request_status,
    rejection_reason = 'CONFLICTING_RENEWAL_ACCEPTED',
    reviewed_at = now(),
    reviewed_by = auth.uid(),
    updated_at = now()
  WHERE previous_protection_id = v_current.id
    AND id <> v_req.id
    AND status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status);

  RETURN v_current.id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.approve_renewal_request(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.approve_renewal_request(uuid) TO authenticated;


-- 4. Correct verify_and_approve_protection_request
CREATE OR REPLACE FUNCTION public.verify_and_approve_protection_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_req record;
BEGIN
  IF NOT (private.has_permission('finance.verify') AND private.has_permission('requests.approve')) THEN
    RAISE EXCEPTION 'PERMISSION_DENIED';
  END IF;

  SELECT * INTO v_req FROM public.protection_requests WHERE id = p_request_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'REQUEST_NOT_FOUND';
  END IF;

  -- 1. Verify payment
  PERFORM public.verify_request_payment(p_request_id, true, 'اعتماد فوري للدفع والطلب');

  -- 2. Route based on request type
  IF v_req.request_type = 'renewal'::public.request_type THEN
    RETURN public.approve_renewal_request(p_request_id);
  ELSE
    RETURN public.approve_protection_request(p_request_id);
  END IF;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.verify_and_approve_protection_request(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.verify_and_approve_protection_request(uuid) TO authenticated;


-- 5. Hardened complete_payment_task
CREATE OR REPLACE FUNCTION public.complete_payment_task(p_task_id uuid, p_telecom_ref text DEFAULT NULL)
RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_task record;
  v_provider uuid;
  v_interval integer;
  v_next timestamptz;
  v_next_amount numeric;
  v_next_currency text;
  v_ref text;
  v_protection record;
BEGIN
  IF NOT private.has_permission('tasks.complete') THEN
    RAISE EXCEPTION 'PERMISSION_DENIED';
  END IF;

  v_ref := nullif(trim(coalesce(p_telecom_ref, '')), '');
  IF v_ref IS NULL THEN
    RAISE EXCEPTION 'TELECOM_REFERENCE_REQUIRED';
  END IF;

  SELECT * INTO v_task FROM public.payment_tasks WHERE id = p_task_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'TASK_NOT_FOUND';
  END IF;

  IF v_task.status = 'completed'::public.task_status THEN
    RETURN true; -- Idempotent
  END IF;

  IF v_task.status = 'cancelled'::public.task_status THEN
    RAISE EXCEPTION 'TASK_CANCELLED';
  END IF;

  IF v_task.status NOT IN ('open'::public.task_status, 'due'::public.task_status) THEN
    RAISE EXCEPTION 'INVALID_TASK_STATUS';
  END IF;

  -- Fetch protection details
  SELECT * INTO v_protection FROM public.protections WHERE id = v_task.protection_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'PROTECTION_NOT_FOUND';
  END IF;

  SELECT coalesce(v_task.provider_id, pn.provider_id) INTO v_provider
  FROM public.customer_numbers cn
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE cn.id = v_protection.customer_number_id;

  -- Mark current task as completed
  UPDATE public.payment_tasks
  SET
    status = 'completed'::public.task_status,
    completed_at = now(),
    completed_by = auth.uid(),
    telecom_reference = v_ref,
    updated_at = now()
  WHERE id = p_task_id;

  -- Record manual payment log and financial expense
  INSERT INTO public.manual_payment_logs (
    task_id,
    amount,
    telecom_reference,
    notes,
    logged_by,
    created_at
  ) VALUES (
    p_task_id,
    coalesce(v_task.amount_snapshot, 0),
    v_ref,
    nullif(v_task.rescheduled_reason, ''),
    auth.uid(),
    now()
  ) ON CONFLICT (task_id) DO NOTHING;

  INSERT INTO public.financial_transactions (
    type,
    amount,
    currency,
    reference_id,
    notes,
    created_by,
    created_at
  ) VALUES (
    'telecom_renewal_expense'::public.transaction_type,
    coalesce(v_task.amount_snapshot, 0),
    coalesce(v_task.amount_currency, 'YER'),
    p_task_id,
    'مصروف تنفيذ مهمة دفع للشبكة',
    auth.uid(),
    now()
  ) ON CONFLICT (reference_id, type) DO NOTHING;

  -- Schedule next recurring task only if protection remains active and future time allows
  IF v_protection.status = 'active'::public.protection_status THEN
    SELECT interval_days INTO v_interval
    FROM public.task_settings
    WHERE provider_id = v_provider AND is_active = true;

    IF v_interval IS NOT NULL AND v_interval > 0 THEN
      v_next := coalesce(v_task.planned_due_at, v_task.due_date) + (v_interval || ' days')::interval;

      IF v_next < v_protection.expires_at THEN
        SELECT amount, currency INTO v_next_amount, v_next_currency
        FROM public.task_amount_settings
        WHERE provider_id = v_provider AND task_type = 'recurring' AND is_active = true
        LIMIT 1;

        INSERT INTO public.payment_tasks (
          protection_id,
          task_type,
          planned_due_at,
          due_date,
          status,
          amount_snapshot,
          amount_currency,
          cycle_number,
          created_at,
          updated_at
        ) VALUES (
          v_task.protection_id,
          'recurring',
          v_next,
          v_next,
          'open'::public.task_status,
          coalesce(v_next_amount, v_task.amount_snapshot, 0),
          coalesce(v_next_currency, v_task.amount_currency, 'YER'),
          coalesce(v_task.cycle_number, 1) + 1,
          now(),
          now()
        ) ON CONFLICT (protection_id, task_type, planned_due_at) DO NOTHING;
      END IF;
    END IF;
  END IF;

  -- Insert audit log
  INSERT INTO public.audit_logs (
    actor_id,
    action,
    entity_type,
    entity_id,
    after_data,
    created_at
  ) VALUES (
    auth.uid(),
    'complete_payment_task',
    'payment_task',
    p_task_id,
    jsonb_build_object('telecom_reference', v_ref, 'amount', v_task.amount_snapshot),
    now()
  );

  RETURN true;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.complete_payment_task(uuid,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.complete_payment_task(uuid,text) TO authenticated;


-- 6. cancel_payment_task
CREATE OR REPLACE FUNCTION public.cancel_payment_task(p_task_id uuid, p_reason text)
RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_task record;
BEGIN
  IF NOT (private.has_permission('tasks.cancel') OR private.has_permission('tasks.complete')) THEN
    RAISE EXCEPTION 'PERMISSION_DENIED';
  END IF;

  IF nullif(trim(p_reason), '') IS NULL THEN
    RAISE EXCEPTION 'CANCELLATION_REASON_REQUIRED';
  END IF;

  SELECT * INTO v_task FROM public.payment_tasks WHERE id = p_task_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'TASK_NOT_FOUND';
  END IF;

  IF v_task.status = 'completed'::public.task_status THEN
    RAISE EXCEPTION 'COMPLETED_TASK_CANNOT_BE_CANCELLED';
  END IF;

  IF v_task.status = 'cancelled'::public.task_status THEN
    RETURN true; -- Idempotent
  END IF;

  IF v_task.status NOT IN ('open'::public.task_status, 'due'::public.task_status) THEN
    RAISE EXCEPTION 'INVALID_TASK_STATUS';
  END IF;

  UPDATE public.payment_tasks
  SET
    status = 'cancelled'::public.task_status,
    rescheduled_reason = trim(p_reason),
    updated_at = now()
  WHERE id = p_task_id;

  INSERT INTO public.audit_logs (
    actor_id,
    action,
    entity_type,
    entity_id,
    after_data,
    created_at
  ) VALUES (
    auth.uid(),
    'cancel_payment_task',
    'payment_task',
    p_task_id,
    jsonb_build_object('reason', trim(p_reason)),
    now()
  );

  RETURN true;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.cancel_payment_task(uuid,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.cancel_payment_task(uuid,text) TO authenticated;


-- 7. Security Hardening: Prevent User Privilege Escalation via Trigger
CREATE OR REPLACE FUNCTION public.prevent_user_privilege_escalation()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
BEGIN
  -- If updated by the user themselves through client REST
  IF auth.uid() IS NOT NULL AND auth.uid() = NEW.id THEN
    -- Check if user has manager/admin permissions
    IF NOT private.has_permission('system.users.manage') THEN
      IF NEW.user_type <> OLD.user_type THEN
        RAISE EXCEPTION 'CANNOT_CHANGE_USER_TYPE';
      END IF;
      IF NEW.role_id IS DISTINCT FROM OLD.role_id THEN
        RAISE EXCEPTION 'CANNOT_CHANGE_ROLE';
      END IF;
      IF NEW.status <> OLD.status THEN
        RAISE EXCEPTION 'CANNOT_CHANGE_STATUS';
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_user_privilege_escalation ON public.users;
CREATE TRIGGER trg_prevent_user_privilege_escalation
  BEFORE UPDATE ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.prevent_user_privilege_escalation();

-- Tighten users_update_self policy
DROP POLICY IF EXISTS users_update_self ON public.users;
CREATE POLICY users_update_self ON public.users
  FOR UPDATE TO authenticated
  USING (id = auth.uid())
  WITH CHECK (id = auth.uid());


-- 8. Updated admin_payment_tasks View
CREATE OR REPLACE VIEW public.admin_payment_tasks AS
SELECT
  pt.id,
  pt.protection_id,
  cn.id AS customer_number_id,
  pn.number,
  tp.id AS provider_id,
  tp.name_ar AS provider_name,
  pt.cycle_number,
  pt.task_type,
  pt.due_date AS due_at,
  pt.amount_snapshot,
  pt.amount_currency,
  pt.status,
  pt.completed_at,
  pt.telecom_reference,
  pt.rescheduled_reason,
  pt.created_at,
  pt.updated_at,
  p.expires_at AS protection_expires_at,
  p.status AS protection_status,
  u.id AS customer_id,
  u.full_name AS customer_name,
  u.phone AS customer_phone,
  (pt.due_date < now() AND pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_overdue,
  (pt.due_date::date = current_date AND pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_due_today,
  (pt.due_date > now() AND pt.due_date <= (now() + interval '7 days') AND pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_due_soon,
  (pt.due_date > (now() + interval '7 days') AND pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_upcoming,
  CASE
    WHEN pt.status = 'completed'::public.task_status THEN 'completed'
    WHEN pt.status = 'cancelled'::public.task_status THEN 'cancelled'
    WHEN pt.due_date < now() THEN 'overdue'
    WHEN pt.due_date::date = current_date THEN 'due'
    WHEN pt.due_date <= (now() + interval '7 days') THEN 'due_soon'
    ELSE 'upcoming'
  END AS time_classification
FROM public.payment_tasks pt
JOIN public.protections p ON p.id = pt.protection_id
JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
JOIN public.telecom_providers tp ON tp.id = pn.provider_id
JOIN public.users u ON u.id = p.customer_id;

GRANT SELECT ON public.admin_payment_tasks TO authenticated;

COMMIT;
