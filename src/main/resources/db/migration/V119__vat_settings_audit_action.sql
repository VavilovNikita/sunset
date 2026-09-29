-- PUT /settings/vat's audit entry (entity type SETTINGS, added by V113). Nothing in this file
-- uses the new value - see CLAUDE.md's Migrations section.
ALTER TYPE "AuditAction" ADD VALUE 'VAT_SETTINGS_UPDATED';
