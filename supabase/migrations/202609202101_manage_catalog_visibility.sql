-- AMAN: make provider/package visibility and package descriptions writable.
-- Defaults preserve compatibility with existing Android callers.

BEGIN;

DROP FUNCTION IF EXISTS public.admin_upsert_provider(uuid,text,text,integer,boolean,integer);
CREATE FUNCTION public.admin_upsert_provider(
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
    VALUES(trim(p_name),trim(p_name),v_code,p_number_length,coalesce(p_is_active,true),coalesce(p_sort_order,0),coalesce(p_is_visible_to_customer,true),nullif(trim(p_logo_url),''))
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.telecom_providers
    SET name_ar=trim(p_name), code=v_code, number_length=p_number_length,
        is_active=coalesce(p_is_active,true), sort_order=coalesce(p_sort_order,0),
        is_visible_to_customer=coalesce(p_is_visible_to_customer,true),
        logo_url=nullif(trim(p_logo_url),''), updated_at=now()
    WHERE id=p_id RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;
  END IF;

  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'upsert_provider','telecom_provider',v_id,
         jsonb_build_object('name',p_name,'code',v_code,'number_length',p_number_length,
                            'is_visible_to_customer',coalesce(p_is_visible_to_customer,true),'logo_url',p_logo_url));
  RETURN v_id;
END;
$$;

DROP FUNCTION IF EXISTS public.admin_upsert_package(uuid,uuid,text,integer,numeric,text,boolean,integer);
CREATE FUNCTION public.admin_upsert_package(
  p_id uuid,
  p_provider_id uuid,
  p_name text,
  p_duration_days integer,
  p_price numeric,
  p_currency text,
  p_is_active boolean,
  p_sort_order integer,
  p_description text DEFAULT NULL,
  p_is_visible_to_customer boolean DEFAULT true
) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_id uuid;
BEGIN
  IF NOT private.has_permission('packages.manage') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.telecom_providers WHERE id=p_provider_id AND is_active) THEN RAISE EXCEPTION 'PROVIDER_NOT_FOUND'; END IF;
  IF nullif(trim(p_name),'') IS NULL THEN RAISE EXCEPTION 'PACKAGE_NAME_REQUIRED'; END IF;
  IF p_duration_days <= 0 OR p_price < 0 THEN RAISE EXCEPTION 'PACKAGE_VALUES_INVALID'; END IF;

  IF p_id IS NULL THEN
    INSERT INTO public.packages(provider_id,name,duration_days,price,currency,is_active,sort_order,description,is_visible_to_customer)
    VALUES(p_provider_id,trim(p_name),p_duration_days,p_price,coalesce(nullif(trim(p_currency),''),'YER'),coalesce(p_is_active,true),coalesce(p_sort_order,0),nullif(trim(p_description),''),coalesce(p_is_visible_to_customer,true))
    RETURNING id INTO v_id;
  ELSE
    UPDATE public.packages
    SET provider_id=p_provider_id,name=trim(p_name),duration_days=p_duration_days,price=p_price,
        currency=coalesce(nullif(trim(p_currency),''),'YER'),is_active=coalesce(p_is_active,true),
        sort_order=coalesce(p_sort_order,0),description=nullif(trim(p_description),''),
        is_visible_to_customer=coalesce(p_is_visible_to_customer,true),updated_at=now()
    WHERE id=p_id RETURNING id INTO v_id;
    IF v_id IS NULL THEN RAISE EXCEPTION 'PACKAGE_NOT_FOUND'; END IF;
  END IF;

  INSERT INTO public.audit_logs(actor_id,action,entity_type,entity_id,after_data)
  VALUES(auth.uid(),'upsert_package','package',v_id,
         jsonb_build_object('provider_id',p_provider_id,'duration_days',p_duration_days,
                            'price',p_price,'currency',p_currency,'description',p_description,
                            'is_visible_to_customer',coalesce(p_is_visible_to_customer,true)));
  RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_upsert_provider(uuid,text,text,integer,boolean,integer,boolean,text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_upsert_package(uuid,uuid,text,integer,numeric,text,boolean,integer,text,boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_upsert_provider(uuid,text,text,integer,boolean,integer,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_package(uuid,uuid,text,integer,numeric,text,boolean,integer,text,boolean) TO authenticated;

COMMIT;
