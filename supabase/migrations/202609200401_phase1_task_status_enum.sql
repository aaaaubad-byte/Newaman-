-- AMAN Phase 1 prerequisite: add task lifecycle values in a committed migration.
BEGIN;
ALTER TYPE public.task_status ADD VALUE IF NOT EXISTS 'open';
ALTER TYPE public.task_status ADD VALUE IF NOT EXISTS 'cancelled';
COMMIT;
