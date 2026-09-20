-- AMAN: preserve the provider English name while keeping the normalized code.
BEGIN;
CREATE OR REPLACE FUNCTION public.admin_upsert_provider(
  p_id uuid,
  p_name text,
  p_code text,
  p_number_length integer,
  p_is_active boolean,
  p_sort_order integer,
  p_is_visible_to_customer boolean DEFAULT true,
  p_logo_url text DEFAULT NULL
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_id uuid; v_code text;
BEGIN
  IF NOT private.has_permission('providers.manage') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF nullif(trim(p_name), '') IS NULL THEN RAISE EXCEPTION 'PROVIDER_NAME_REQUIRED'; END IF;
  IF p_number_length NOT BETWEEN 7 AND 15 THEN RAISE EXCEPTION 'NUMBER_LENGTH_INVALID'; END IF;
  v_code := lower(trim(p_code));
  IF v_code = '' THEN RAISE EXCEPTION 'PROVIDER_CODE_REQUIRED'; END IF;
  IF p_id IS NULL THEN
    INSERT INTO public.telecom_providers(name_ar,name_en,code,number_length,is_active,sort_order,is_visible_to_customer,logo_url)
    VALUES(trim(p_name),trim(p_code),v_code,p_number_length,coalesce(p_is_active,true),coalesce(p_sort_order,0),coalesce(p_is_visible_to_customer,true),nullif(trim(p_logo_url),''))
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.telecom_providers
    SET name_ar=trim(p_name), name_en=trim(p_code), code=v_code, number_length=p_number_length,
        is_active=coalesce(p_is_active,true), sort_order=coalesce(p_sort_order,0),
        is_visible_to_customer=coalesce(p_is_visible_to_customer,true), logo_url=nullif(trim(p_logo_url),''), updated_at=now()
    WHERE id=p_id RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;
  END IF;
  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'upsert_provider','telecom_provider',v_id,jsonb_build_object('name',p_name,'code',v_code,'name_en',p_code,'is_visible_to_customer',coalesce(p_is_visible_to_customer,true),'logo_url',p_logo_url));
  RETURN v_id;
END;
$$;
COMMIT;
