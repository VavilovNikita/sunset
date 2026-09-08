-- New audit actions/entity type for maintenance tasks (see V35__maintenance_tasks.sql). No
-- backfill needed - same reasoning as V19/V22/V25/V27/V30/V34: only ever written by future
-- actions, never queried within this same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'MAINTENANCE_TASK_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'MAINTENANCE_TASK_STATUS_CHANGED';
ALTER TYPE "AuditAction" ADD VALUE 'MAINTENANCE_TASK_BLOCKED';
ALTER TYPE "AuditAction" ADD VALUE 'MAINTENANCE_TASK_BLOCK_LIFTED';

ALTER TYPE "AuditEntityType" ADD VALUE 'MAINTENANCE_TASK';
