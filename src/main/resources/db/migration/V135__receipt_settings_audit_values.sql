-- Audit values for the receipt designer (GET/PUT /settings/receipt, PUT/DELETE its logo). Entity
-- type is the existing SETTINGS (V113). Nothing in this file uses the new values - see
-- CLAUDE.md's Migrations section for why ADD VALUE gets its own migration.
ALTER TYPE "AuditAction" ADD VALUE 'RECEIPT_SETTINGS_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'RECEIPT_LOGO_CHANGED';
