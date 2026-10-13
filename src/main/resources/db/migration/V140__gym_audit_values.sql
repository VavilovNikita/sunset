-- Separate migration file from anything that uses these values - see CLAUDE.md's Migrations
-- section: Postgres forbids using a freshly added enum value in the same transaction.
ALTER TYPE "AuditAction" ADD VALUE 'GYM_DEVICE_COMMAND';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_DEVICE_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_DEVICE_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_DEVICE_DELETED';
ALTER TYPE "AuditEntityType" ADD VALUE 'GYM_DEVICE';
