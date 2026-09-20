-- AMAN Phase 1 correction: close nullable snapshot gaps, revoke anonymous RPC execution,
-- and provide the required server-side renewal approval contract.

BEGIN;

ALTER TABLE public.protection_requests
  ALTER COLUMN price_snapshot SET NOT NULL,
  ALTER COLUMN duration_days_snapshot SET NOT NULL,
  ALTER COLUMN currency_snapshot SET NOT NULL;

ALTER TABLE public.protections
  ALTER COLUMN package_price_snapshot SET NOT NULL,
  ALTER COLUMN package_duration_days_snapshot SET NOT NULL,
  ALTER COLUMN package_currency_snapshot SET NOT NULL;

ALTER TABLE public.payment_tasks
  ALTER COLUMN amount_snapshot SET NOT NULL,
  ALTER COLUMN amount_currency SET NOT NULL;

CREATE OR REPLACE FUNCTION public.approve_renewal_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_req record;
  v_current record;
  v_pkg record;
  v_provider_id uuid;
  v_new_expiry timestamptz;
BEGIN
  IF NOT private.has_permission('requests.approve') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;

  SELECT * INTO v_req
  FROM public.protection_requests
  WHERE id = p_request_id
  FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_req.request_type <> 'renewal' OR v_req.previous_protection_id IS NULL THEN RAISE EXCEPTION 'INVALID_RENEWAL_REQUEST'; END IF;
  IF v_req.status <> 'under_review' OR v_req.payment_verified_at IS NULL THEN RAISE EXCEPTION 'PAYMENT_NOT_VERIFIED'; END IF;

  SELECT p.*, cn.customer_id, pn.provider_id AS number_provider_id
    INTO v_current
  FROM public.protections p
  JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE p.id = v_req.previous_protection_id
  FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'PROTECTION_NOT_FOUND'; END IF;
  IF v_current.status <> 'active'::public.protection_status THEN RAISE EXCEPTION 'PROTECTION_NOT_ACTIVE'; END IF;
  IF v_current.customer_id <> v_req.customer_id THEN RAISE EXCEPTION 'PROTECTION_OWNER_MISMATCH'; END IF;
  IF v_current.customer_number_id <> v_req.customer_number_id THEN RAISE EXCEPTION 'PROTECTION_NUMBER_MISMATCH'; END IF;

  SELECT * INTO v_pkg FROM public.packages WHERE id = v_req.package_id AND is_active;
  IF NOT FOUND THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_pkg.provider_id <> v_current.number_provider_id THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;

  IF EXISTS (
    SELECT 1 FROM public.protection_requests
    WHERE previous_protection_id = v_req.previous_protection_id
      AND id <> p_request_id
      AND status IN ('pending_payment_verification','under_review')
  ) THEN
    RAISE EXCEPTION 'RENEWAL_CONFLICT';
  END IF;

  v_new_expiry := v_current.expires_at + (v_req.duration_days_snapshot || ' days')::interval;
  UPDATE public.protections
  SET package_id = v_req.package_id,
      expires_at = v_new_expiry,
      package_price_snapshot = v_req.price_snapshot,
      package_duration_days_snapshot = v_req.duration_days_snapshot,
      package_currency_snapshot = v_req.currency_snapshot,
      status = 'active'::public.protection_status,
      updated_at = now()
  WHERE id = v_req.previous_protection_id;

  UPDATE public.protection_requests
  SET status = 'approved', reviewed_at = now(), reviewed_by = auth.uid(), updated_at = now()
  WHERE id = p_request_id;

  INSERT INTO public.protection_history(protection_id, event, event_details)
  VALUES (v_req.previous_protection_id, 'renewal_approved', jsonb_build_object(
    'request_id', p_request_id,
    'package_id', v_req.package_id,
    'price_snapshot', v_req.price_snapshot,
    'duration_days_snapshot', v_req.duration_days_snapshot,
    'currency_snapshot', v_req.currency_snapshot,
    'previous_expires_at', v_current.expires_at,
    'new_expires_at', v_new_expiry
  ));

  INSERT INTO public.financial_transactions(type, amount, currency, reference_id, notes, created_by)
  VALUES ('customer_payment', v_req.price_snapshot, v_req.currency_snapshot, v_req.previous_protection_id, 'رسوم تجديد معتمدة', auth.uid());
  INSERT INTO public.client_notifications(customer_id, title, message, type, related_entity_type, related_entity_id)
  VALUES (v_req.customer_id, 'تم قبول التجديد', 'تم تمديد الحماية حتى تاريخ الانتهاء الجديد', 'renewal_approved', 'protection', v_req.previous_protection_id);
  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'approve_renewal_request', 'protection', v_req.previous_protection_id,
          jsonb_build_object('request_id', p_request_id, 'new_expires_at', v_new_expiry));

  RETURN v_req.previous_protection_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.admin_upsert_task_classification(uuid,uuid,text,integer,integer,integer,boolean) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.admin_upsert_notification_setting(uuid,text,text,boolean,integer) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.approve_renewal_request(uuid) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.submit_renewal_request(uuid,uuid,uuid,text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.verify_request_payment(uuid,boolean,text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.reject_protection_request(uuid,text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.approve_protection_request(uuid) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.complete_payment_task(uuid,text) FROM PUBLIC, anon;

GRANT EXECUTE ON FUNCTION public.admin_upsert_task_classification(uuid,uuid,text,integer,integer,integer,boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_notification_setting(uuid,text,text,boolean,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_renewal_request(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_renewal_request(uuid,uuid,uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.verify_request_payment(uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_protection_request(uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_protection_request(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_payment_task(uuid,text) TO authenticated;

COMMIT;
