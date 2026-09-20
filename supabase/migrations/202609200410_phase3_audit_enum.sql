-- AMAN Phase 3 prerequisite: audit action for task cancellation.
BEGIN;
ALTER TYPE public.audit_action ADD VALUE IF NOT EXISTS 'cancel_payment_task';
COMMIT;
