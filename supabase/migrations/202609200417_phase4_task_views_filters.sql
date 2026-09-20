-- AMAN Phase 4: task source view, visibility flags, filters, and ordering.

BEGIN;

DROP VIEW IF EXISTS public.admin_payment_tasks;

CREATE VIEW public.admin_payment_tasks
WITH (security_invoker=true)
AS
WITH base AS (
  SELECT
    pt.id,
    pt.protection_id,
    p.customer_number_id,
    COALESCE(pt.provider_id, pn.provider_id) AS provider_id,
    u.full_name AS customer_name,
    pn.number,
    tp.name_ar AS provider_name,
    pt.task_type,
    pt.due_date AS due_at,
    COALESCE(pt.telecom_due_at, pt.due_date) AS telecom_due_at,
    pt.status,
    pt.completed_at,
    pt.completed_by,
    pt.telecom_reference,
    pt.rescheduled_reason,
    pt.scheduled_at,
    pt.created_at,
    pt.updated_at,
    pt.amount_snapshot,
    pt.amount_currency,
    pt.cycle_number,
    pt.visible_from,
    pt.planned_due_at,
    COALESCE(ts.visibility_days_before, 0) AS visibility_days_before,
    CEIL(EXTRACT(epoch FROM (COALESCE(pt.telecom_due_at, pt.due_date) - now())) / 86400.0)::integer AS days_remaining,
    (COALESCE(pt.telecom_due_at, pt.due_date) < now()
      AND pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_overdue,
    (COALESCE(pt.telecom_due_at, pt.due_date)::date = current_date) AS is_due_today,
    (pt.status IN ('completed'::public.task_status, 'cancelled'::public.task_status)
      OR COALESCE(pt.telecom_due_at, pt.due_date) < now()
      OR pt.visible_from IS NULL
      OR pt.visible_from <= now()) AS is_visible_now,
    (pt.status IN ('open'::public.task_status, 'due'::public.task_status)) AS is_open,
    (pt.status = 'completed'::public.task_status) AS is_completed,
    (pt.status = 'cancelled'::public.task_status) AS is_cancelled,
    CASE
      WHEN pt.status IN ('open'::public.task_status, 'due'::public.task_status)
        AND COALESCE(pt.telecom_due_at, pt.due_date) < now() THEN 'overdue'
      ELSE pt.status::text
    END AS calculated_status,
    cls.name AS classification_name,
    cls.sort_order AS classification_sort_order
  FROM public.payment_tasks pt
  JOIN public.protections p ON p.id = pt.protection_id
  JOIN public.customer_numbers cn ON cn.id = p.customer_number_id
  JOIN public.users u ON u.id = cn.customer_id
  JOIN public.phone_numbers pn ON pn.id = cn.phone_number_id
  JOIN public.telecom_providers tp ON tp.id = COALESCE(pt.provider_id, pn.provider_id)
  LEFT JOIN public.task_settings ts
    ON ts.provider_id = COALESCE(pt.provider_id, pn.provider_id)
   AND ts.is_active
  LEFT JOIN LATERAL (
    SELECT c.name, c.sort_order
    FROM public.task_time_classifications c
    WHERE c.task_settings_id = ts.id
      AND c.is_active
      AND (c.min_days_remaining IS NULL OR CEIL(EXTRACT(epoch FROM (COALESCE(pt.telecom_due_at, pt.due_date) - now())) / 86400.0)::integer >= c.min_days_remaining)
      AND (c.max_days_remaining IS NULL OR CEIL(EXTRACT(epoch FROM (COALESCE(pt.telecom_due_at, pt.due_date) - now())) / 86400.0)::integer <= c.max_days_remaining)
    ORDER BY c.sort_order, c.id
    LIMIT 1
  ) cls ON true
)
SELECT
  b.id,
  b.protection_id,
  b.customer_number_id,
  b.provider_id,
  b.customer_name,
  b.number,
  b.provider_name,
  b.task_type,
  b.due_at,
  b.telecom_due_at,
  b.days_remaining,
  b.calculated_status,
  b.status,
  b.completed_at,
  b.completed_by,
  b.telecom_reference,
  b.rescheduled_reason,
  b.scheduled_at,
  b.created_at,
  b.updated_at,
  b.amount_snapshot,
  b.amount_currency,
  b.cycle_number,
  b.visible_from,
  b.planned_due_at,
  b.visibility_days_before,
  b.is_overdue,
  b.is_due_today,
  b.is_visible_now,
  b.is_open,
  b.is_completed,
  b.is_cancelled,
  b.classification_name,
  b.classification_sort_order,
  CASE WHEN b.is_overdue THEN 0 WHEN b.is_due_today THEN 1 ELSE 2 END AS urgency_sort
FROM base b;

CREATE OR REPLACE FUNCTION public.get_admin_payment_tasks(
  p_provider_id uuid DEFAULT NULL,
  p_task_type text DEFAULT NULL,
  p_status text DEFAULT NULL,
  p_from timestamptz DEFAULT NULL,
  p_to timestamptz DEFAULT NULL,
  p_sort text DEFAULT 'urgency',
  p_desc boolean DEFAULT false,
  p_limit integer DEFAULT 200,
  p_offset integer DEFAULT 0
) RETURNS SETOF public.admin_payment_tasks
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_limit integer := greatest(1, least(coalesce(p_limit, 200), 500));
DECLARE v_offset integer := greatest(0, coalesce(p_offset, 0));
BEGIN
  IF NOT private.has_permission('tasks.read') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF p_status IS NOT NULL AND p_status NOT IN ('open','completed','cancelled','overdue','due_today') THEN RAISE EXCEPTION 'INVALID_TASK_STATUS_FILTER'; END IF;
  IF p_sort IS NOT NULL AND p_sort NOT IN ('urgency','due_at','provider','task_type','created_at') THEN RAISE EXCEPTION 'INVALID_TASK_SORT'; END IF;
  IF p_from IS NOT NULL AND p_to IS NOT NULL AND p_from > p_to THEN RAISE EXCEPTION 'INVALID_TASK_DATE_RANGE'; END IF;

  RETURN QUERY
  SELECT t.*
  FROM public.admin_payment_tasks t
  WHERE (p_provider_id IS NULL OR t.provider_id = p_provider_id)
    AND (p_task_type IS NULL OR t.task_type = p_task_type)
    AND (p_status IS NULL
      OR (p_status='due_today' AND t.is_due_today)
      OR (p_status<>'due_today' AND t.calculated_status=p_status))
    -- A manual date range intentionally overrides the default visibility window.
    AND (p_from IS NOT NULL OR p_to IS NOT NULL OR t.is_visible_now)
    AND (p_from IS NULL OR t.due_at >= p_from)
    AND (p_to IS NULL OR t.due_at <= p_to)
  ORDER BY
    CASE WHEN p_sort='urgency' AND NOT p_desc THEN t.urgency_sort END ASC,
    CASE WHEN p_sort='urgency' AND p_desc THEN t.urgency_sort END DESC,
    CASE WHEN p_sort='due_at' AND NOT p_desc THEN t.due_at END ASC,
    CASE WHEN p_sort='due_at' AND p_desc THEN t.due_at END DESC,
    CASE WHEN p_sort='provider' AND NOT p_desc THEN t.provider_name END ASC,
    CASE WHEN p_sort='provider' AND p_desc THEN t.provider_name END DESC,
    CASE WHEN p_sort='task_type' AND NOT p_desc THEN t.task_type END ASC,
    CASE WHEN p_sort='task_type' AND p_desc THEN t.task_type END DESC,
    CASE WHEN p_sort='created_at' AND NOT p_desc THEN t.created_at END ASC,
    CASE WHEN p_sort='created_at' AND p_desc THEN t.created_at END DESC,
    t.id
  LIMIT v_limit OFFSET v_offset;
END;
$$;

CREATE OR REPLACE FUNCTION public.count_admin_payment_tasks(
  p_provider_id uuid DEFAULT NULL,
  p_task_type text DEFAULT NULL,
  p_status text DEFAULT NULL,
  p_from timestamptz DEFAULT NULL,
  p_to timestamptz DEFAULT NULL
) RETURNS bigint
LANGUAGE plpgsql SECURITY DEFINER SET search_path TO ''
AS $$
DECLARE v_count bigint;
BEGIN
  IF NOT private.has_permission('tasks.read') THEN RAISE EXCEPTION 'PERMISSION_DENIED'; END IF;
  IF p_status IS NOT NULL AND p_status NOT IN ('open','completed','cancelled','overdue','due_today') THEN RAISE EXCEPTION 'INVALID_TASK_STATUS_FILTER'; END IF;
  IF p_from IS NOT NULL AND p_to IS NOT NULL AND p_from > p_to THEN RAISE EXCEPTION 'INVALID_TASK_DATE_RANGE'; END IF;
  SELECT count(*) INTO v_count
  FROM public.admin_payment_tasks t
  WHERE (p_provider_id IS NULL OR t.provider_id=p_provider_id)
    AND (p_task_type IS NULL OR t.task_type=p_task_type)
    AND (p_status IS NULL OR (p_status='due_today' AND t.is_due_today) OR (p_status<>'due_today' AND t.calculated_status=p_status))
    AND (p_from IS NOT NULL OR p_to IS NOT NULL OR t.is_visible_now)
    AND (p_from IS NULL OR t.due_at >= p_from)
    AND (p_to IS NULL OR t.due_at <= p_to);
  RETURN v_count;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.get_admin_payment_tasks(uuid,text,text,timestamptz,timestamptz,text,boolean,integer,integer) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.count_admin_payment_tasks(uuid,text,text,timestamptz,timestamptz) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_admin_payment_tasks(uuid,text,text,timestamptz,timestamptz,text,boolean,integer,integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.count_admin_payment_tasks(uuid,text,text,timestamptz,timestamptz) TO authenticated;

COMMIT;
