-- Separate migration file from anything that uses these values - see CLAUDE.md's Migrations
-- section: Postgres forbids using a freshly added enum value in the same transaction.
ALTER TYPE "AuditAction" ADD VALUE 'LIGHTING_DEVICE_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'LIGHTING_DEVICE_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'LIGHTING_DEVICE_DELETED';
ALTER TYPE "AuditAction" ADD VALUE 'LIGHTING_DEVICE_COMMAND';
ALTER TYPE "AuditEntityType" ADD VALUE 'LIGHTING_DEVICE';
