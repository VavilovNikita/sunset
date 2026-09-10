-- Its own migration, separate from V44/V45 (which create the table/column these describe) and
-- from any DML using them - see the ALTER TYPE ... ADD VALUE split rule (V2/V3, V6/V7).
ALTER TYPE "AuditAction" ADD VALUE 'GUEST_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'GUEST_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'GUEST_DELETED';
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_GUEST_LINKED';

-- New AuditEntityType value these actions file under - same reasoning as V30/V32/V36/V42.
ALTER TYPE "AuditEntityType" ADD VALUE 'GUEST';
