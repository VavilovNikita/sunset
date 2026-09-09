-- Its own migration, separate from V40/V41 (which create the columns/table these describe) and
-- from any DML using them - see the ALTER TYPE ... ADD VALUE split rule (V2/V3, V6/V7). Several
-- values in one file, same as V2's own CASHIER + WAITER.
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_STATUS_CHANGED';
ALTER TYPE "AuditAction" ADD VALUE 'POS_TABLE_POSITION_UPDATED';

-- New AuditEntityType values these actions file under - same reasoning as V30/V32/V36.
ALTER TYPE "AuditEntityType" ADD VALUE 'TABLE';
ALTER TYPE "AuditEntityType" ADD VALUE 'SPA_APPOINTMENT';
