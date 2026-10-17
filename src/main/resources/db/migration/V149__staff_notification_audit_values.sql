-- Separate migration file from anything that uses these values - see CLAUDE.md's Migrations
-- section: Postgres forbids using a freshly added enum value in the same transaction.
ALTER TYPE "AuditAction" ADD VALUE 'NOTIFICATION_SETTINGS_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'NOTIFICATION_LINK_CODE_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'NOTIFICATION_CHANNEL_LINKED';
ALTER TYPE "AuditAction" ADD VALUE 'NOTIFICATION_CHANNEL_UNLINKED';
ALTER TYPE "AuditAction" ADD VALUE 'NOTIFICATION_LANGUAGE_UPDATED';
