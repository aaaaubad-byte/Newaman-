-- AMAN live-contract corrections.
-- Evidence-based fixes against the live New aman schema:
-- 1) block new protection requests for numbers with an active protection;
-- 2) expose protection snapshots through the customer view;
-- 3) separate provider activation from customer visibility.

BEGIN;

ALTER TABLE public.telecom_providers
  ADD COLUMN IF NOT EXISTS logo_url text,
  ADD COLUMN IF NOT EXISTS is_visible_to_customer boolean NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS telecom_providers_customer_visibility_idx
  ON public.telecom_providers(is_active, is_visible_to_customer, sort_order);

CREATE OR REPLACE FUNCTION public.submit_protection_request(
  p_number_id uuid,
  p_package_id uuid,
  p_wallet_id uuid,
  p_transfer_ref text,
  p_idempotency_key text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE
  v_request_id uuid;
  v_existing uuid;
  v_number_provider uuid;
  v_package_provider uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'UNAUTHENTICATED'; END IF;

  SELECT pn.provider_id INTO v_number_provider
  FROM public.customer_numbers cn
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  WHERE cn.id = p_number_id
    AND cn.customer_id = auth.uid()
    AND cn.is_active;
  IF v_number_provider IS NULL THEN RAISE EXCEPTION 'NUMBER_NOT_FOUND'; END IF;

  -- A number may be listed by multiple customers, but it may not receive a
  -- new protection request while one active protection already exists.
  IF EXISTS (
    SELECT 1
    FROM public.protections
    WHERE customer_number_id = p_number_id
      AND status = 'active'::public.protection_status
  ) THEN
    RAISE EXCEPTION 'NUMBER_ALREADY_PROTECTED';
  END IF;

  SELECT provider_id INTO v_package_provider
  FROM public.packages
  WHERE id = p_package_id
    AND is_active
    AND is_visible_to_customer;
  IF v_package_provider IS NULL THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  IF v_number_provider <> v_package_provider THEN RAISE EXCEPTION 'PACKAGE_PROVIDER_MISMATCH'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.payment_methods WHERE id = p_wallet_id AND is_active) THEN
    RAISE EXCEPTION 'PAYMENT_METHOD_NOT_FOUND';
  END IF;
  IF nullif(trim(p_transfer_ref), '') IS NULL THEN RAISE EXCEPTION 'TRANSFER_REFERENCE_REQUIRED'; END IF;

  IF p_idempotency_key IS NOT NULL THEN
    SELECT id INTO v_existing
    FROM public.protection_requests
    WHERE idempotency_key = p_idempotency_key;
    IF v_existing IS NOT NULL THEN RETURN v_existing; END IF;
  END IF;

  INSERT INTO public.protection_requests(
    customer_id,
    customer_number_id,
    package_id,
    payment_method_id,
    transfer_reference,
    idempotency_key,
    request_type,
    price_snapshot,
    duration_days_snapshot,
    currency_snapshot,
    status
  )
  SELECT auth.uid(), p_number_id, p_package_id, p_wallet_id, trim(p_transfer_ref),
         p_idempotency_key, 'new', p.price, p.duration_days, p.currency,
         'pending_payment_verification'::public.request_status
  FROM public.packages p
  WHERE p.id = p_package_id
  RETURNING id INTO v_request_id;

  INSERT INTO public.admin_notifications(
    title, message, type, related_entity_type, related_entity_id
  ) VALUES (
    'طلب حماية جديد',
    'تم تقديم طلب حماية جديد يتطلب مراجعة الدفع',
    'request_submitted',
    'protection_request',
    v_request_id
  );

  INSERT INTO public.audit_logs(actor_id, action, entity_type, entity_id)
  VALUES (auth.uid(), 'submit_protection_request', 'protection_request', v_request_id);
  RETURN v_request_id;
END;
$$;

DROP VIEW IF EXISTS public.customer_my_protections;
CREATE VIEW public.customer_my_protections
WITH (security_invoker=true)
AS
SELECT
  p.id,
  p.request_id,
  cn.customer_id,
  p.customer_number_id,
  pn.number,
  tp.name_ar AS provider_name,
  p.package_id,
  pkg.name AS package_name,
  p.package_price_snapshot,
  p.package_duration_days_snapshot,
  p.package_currency_snapshot,
  p.starts_at,
  p.expires_at,
  p.status,
  p.created_at
FROM public.protections p
JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
JOIN public.telecom_providers tp ON tp.id = pn.provider_id
JOIN public.packages pkg ON pkg.id = p.package_id
WHERE cn.customer_id = auth.uid();

GRANT SELECT ON public.customer_my_protections TO authenticated;
REVOKE ALL ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_protection_request(uuid,uuid,uuid,text,text) TO authenticated;

COMMIT;
