-- New audit action for job-function assignment changes (see V33__user_job_functions.sql). No
-- backfill needed - same reasoning as V19/V22/V25/V27/V30: only ever written by future actions,
-- never queried within this same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'USER_FUNCTIONS_CHANGED';
