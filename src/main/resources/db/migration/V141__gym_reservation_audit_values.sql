-- Separate migration file from anything that uses these values - see CLAUDE.md's Migrations
-- section: Postgres forbids using a freshly added enum value in the same transaction.
ALTER TYPE "AuditAction" ADD VALUE 'GYM_RESERVATION_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_RESERVATION_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_RESERVATION_CANCELLED';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_AUTOMATION_RESULT';
ALTER TYPE "AuditAction" ADD VALUE 'GYM_HALL_SETTINGS_UPDATED';
ALTER TYPE "AuditEntityType" ADD VALUE 'GYM_RESERVATION';
ALTER TYPE "AuditEntityType" ADD VALUE 'GYM_HALL_SETTINGS';
