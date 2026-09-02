-- New audit action for closing a FAILED print job as not-actionable (see V31). No backfill needed
-- - same reasoning as V19/V22/V25/V27/V30: only ever written by future actions, never queried
-- within this same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'PRINT_JOB_DISMISSED';
ALTER TYPE "AuditEntityType" ADD VALUE 'PRINT_JOB';
