-- PUT /settings/lifecycle-emails' audit entry. Nothing in this file uses either new value - see
-- CLAUDE.md's Migrations section: Postgres forbids using a freshly added enum value in the same
-- transaction. SETTINGS is the entity type (no existing one fits a singleton configuration row).
ALTER TYPE "AuditAction" ADD VALUE 'LIFECYCLE_EMAIL_SETTINGS_UPDATED';
ALTER TYPE "AuditEntityType" ADD VALUE 'SETTINGS';
