-- AMAN Phase 2: protection and renewal workflow hardening.
-- All changes are transactional and preserve prior protection history.

BEGIN;

-- Only one pending renewal may exist for a protection. New protection requests
-- remain independently addable for the same phone number.
CREATE UNIQUE INDEX IF NOT EXISTS protection_requests_one_pending_renewal_uidx
  ON public.protection_requests(previous_protection_id)
  WHERE request_type = 'renewal'
    AND status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status);

CREATE OR REPLACE FUNCTION public.submit_renewal_request(
  p_protection_id uuid, p_package_id uuid, p_wallet_id uuid, p_transfer_ref text
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_current record;
  v_package record;
  v_request_id uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'UNAUTHENTICATED'; END IF;
  IF nullif(trim(p_transfer_ref), '') IS NULL THEN RAISE EXCEPTION 'TRANSFER_REFERENCE_REQUIRED'; END IF;

  -- Lock the protection before checking ownership, status, and pending requests.
  SELECT p.id, p.customer_number_id, p.expires_at, p.status,
         cn.customer_id, pn.provider_id AS number_provider_id
    INTO v_current
  FROM public.protections p
  JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE p.id = p_protection_id
  FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'PROTECTION_NOT_FOUND'; END IF;
  IF v_current.customer_id <> auth.uid() THEN RAISE EXCEPTION 'PROTECTION_OWNER_MISMATCH'; END IF;
  IF v_current.status <> 'active'::public.protection_status THEN RAISE EXCEPTION 'PROTECTION_NOT_ACTIVE'; END IF;

  IF EXISTS (
    SELECT 1 FROM public.protection_requests
    WHERE previous_protection_id = p_protection_id
      AND request_type = 'renewal'
      AND status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status)
  ) THEN
    RAISE EXCEPTION 'RENEWAL_ALREADY_PENDING';
  END IF;

  SELECT * INTO v_package
  FROM public.packages
  WHERE id = p_package_id AND is_active AND is_visible_to_customer;
  IF NOT FOUND THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_package.provider_id <> v_current.number_provider_id THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.payment_methods WHERE id=p_wallet_id AND is_active) THEN RAISE EXCEPTION 'PAYMENT_METHOD_NOT_FOUND'; END IF;

  BEGIN
    INSERT INTO public.protection_requests(
      customer_id, customer_number_id, package_id, payment_method_id, transfer_reference,
      request_type, previous_protection_id, price_snapshot, duration_days_snapshot, currency_snapshot, status
    ) VALUES (
      v_current.customer_id, v_current.customer_number_id, p_package_id, p_wallet_id, trim(p_transfer_ref),
      'renewal', p_protection_id, v_package.price, v_package.duration_days, v_package.currency,
      'pending_payment_verification'::public.request_status
    ) RETURNING id INTO v_request_id;
  EXCEPTION WHEN unique_violation THEN
    RAISE EXCEPTION 'RENEWAL_ALREADY_PENDING';
  END;

  INSERT INTO public.admin_notifications(title, message, type, related_entity_type, related_entity_id)
  VALUES ('طلب تجديد جديد', 'تم تقديم طلب تجديد مرتبط بحماية سارية', 'renewal_submitted', 'protection_request', v_request_id);
  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id, after_data)
  VALUES (auth.uid(), 'submit_renewal_request', 'protection_request', v_request_id,
          jsonb_build_object('protection_id', p_protection_id, 'package_id', p_package_id));
  RETURN v_request_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.approve_renewal_request(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_req record;
  v_current record;
  v_package record;
  v_new_expiry timestamptz;
BEGIN
  IF NOT private.has_permission('requests.approve') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;

  SELECT * INTO v_req FROM public.protection_requests WHERE id=p_request_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_req.request_type <> 'renewal' OR v_req.previous_protection_id IS NULL THEN RAISE EXCEPTION 'INVALID_RENEWAL_REQUEST'; END IF;
  IF v_req.status <> 'under_review' OR v_req.payment_verified_at IS NULL THEN RAISE EXCEPTION 'PAYMENT_NOT_VERIFIED'; END IF;

  -- Lock the current protection so two managers cannot extend it concurrently.
  SELECT p.*, cn.customer_id, cn.phone_number_id, pn.provider_id AS number_provider_id
    INTO v_current
  FROM public.protections p
  JOIN public.customer_numbers cn ON cn.id=p.customer_number_id
  JOIN public.phone_numbers pn ON pn.id=cn.phone_number_id
  WHERE p.id=v_req.previous_protection_id
  FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'PROTECTION_NOT_FOUND'; END IF;
  IF v_current.status <> 'active'::public.protection_status THEN RAISE EXCEPTION 'PROTECTION_NOT_ACTIVE'; END IF;
  IF v_current.customer_id <> v_req.customer_id THEN RAISE EXCEPTION 'PROTECTION_OWNER_MISMATCH'; END IF;
  IF v_current.customer_number_id <> v_req.customer_number_id THEN RAISE EXCEPTION 'PROTECTION_NUMBER_MISMATCH'; END IF;

  SELECT * INTO v_package FROM public.packages WHERE id=v_req.package_id AND is_active;
  IF NOT FOUND THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_package.provider_id <> v_current.number_provider_id THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;

  v_new_expiry := v_current.expires_at + (v_req.duration_days_snapshot || ' days')::interval;

  UPDATE public.protections
  SET package_id=v_req.package_id,
      expires_at=v_new_expiry,
      package_price_snapshot=v_req.price_snapshot,
      package_duration_days_snapshot=v_req.duration_days_snapshot,
      package_currency_snapshot=v_req.currency_snapshot,
      status='active'::public.protection_status,
      updated_at=now()
  WHERE id=v_req.previous_protection_id;

  UPDATE public.protection_requests
  SET status='approved', reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now()
  WHERE id=p_request_id;

  INSERT INTO public.protection_history(protection_id,event,event_details)
  VALUES (v_req.previous_protection_id,'renewal_approved',jsonb_build_object(
    'request_id',p_request_id,
    'package_id',v_req.package_id,
    'price_snapshot',v_req.price_snapshot,
    'duration_days_snapshot',v_req.duration_days_snapshot,
    'currency_snapshot',v_req.currency_snapshot,
    'previous_expires_at',v_current.expires_at,
    'new_expires_at',v_new_expiry
  ));

  INSERT INTO public.financial_transactions(type,amount,currency,reference_id,notes,created_by)
  VALUES ('customer_payment',v_req.price_snapshot,v_req.currency_snapshot,v_req.previous_protection_id,'رسوم تجديد معتمدة',auth.uid());
  INSERT INTO public.client_notifications(customer_id,title,message,type,related_entity_type,related_entity_id)
  VALUES (v_req.customer_id,'تم قبول التجديد','تم تمديد الحماية من تاريخ انتهائها الحالي حتى تاريخ الانتهاء الجديد','renewal_approved','protection',v_req.previous_protection_id);
  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES (auth.uid(),'approve_renewal_request','protection',v_req.previous_protection_id,
          jsonb_build_object('request_id',p_request_id,'previous_expires_at',v_current.expires_at,'new_expires_at',v_new_expiry));

  -- A winning renewal closes other pending requests for the same protection.
  UPDATE public.protection_requests
  SET status='rejected', rejection_reason='CONFLICTING_RENEWAL_ACCEPTED',
      reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now()
  WHERE previous_protection_id=v_req.previous_protection_id
    AND id<>p_request_id
    AND status IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status);

  RETURN v_req.previous_protection_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.verify_request_payment(
  p_request_id uuid, p_verified boolean, p_note text DEFAULT NULL
) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_req record; v_reason text;
BEGIN
  IF NOT private.has_permission('finance.verify') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  SELECT * INTO v_req FROM public.protection_requests WHERE id=p_request_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'REQUEST_NOT_FOUND'; END IF;
  IF v_req.status NOT IN ('pending_payment_verification'::public.request_status, 'under_review'::public.request_status) THEN RAISE EXCEPTION 'INVALID_REQUEST_STATUS'; END IF;

  IF p_verified THEN
    UPDATE public.protection_requests
    SET status='under_review', payment_verified_at=now(), payment_verified_by=auth.uid(), verified_note=p_note, updated_at=now()
    WHERE id=p_request_id;
  ELSE
    v_reason := coalesce(nullif(trim(p_note), ''), 'TRANSFER_NOT_RECEIVED');
    UPDATE public.protection_requests
    SET status='rejected', rejection_reason=v_reason, payment_verified_by=auth.uid(), verified_note=p_note,
        reviewed_at=now(), reviewed_by=auth.uid(), updated_at=now()
    WHERE id=p_request_id;
    INSERT INTO public.client_notifications(customer_id,title,message,type,related_entity_type,related_entity_id)
    VALUES (v_req.customer_id,'تم رفض التحقق من التحويل',v_reason,'request_rejected','protection_request',p_request_id);
  END IF;

  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES (auth.uid(),'verify_payment','protection_request',p_request_id,
          jsonb_build_object('verified',p_verified,'note',p_note));
  RETURN true;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.submit_renewal_request(uuid,uuid,uuid,text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.approve_renewal_request(uuid) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.verify_request_payment(uuid,boolean,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_renewal_request(uuid,uuid,uuid,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_renewal_request(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.verify_request_payment(uuid,boolean,text) TO authenticated;

COMMIT;
