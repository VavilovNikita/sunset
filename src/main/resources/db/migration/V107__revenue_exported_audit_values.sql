-- GET /reports/revenue-export's audit entry. Nothing in this file uses either new value - see
-- CLAUDE.md's Migrations section: Postgres forbids using a freshly added enum value in the same
-- transaction. PAYMENT is the entity type: the export reads Payment and FolioPayment rows, and no
-- existing type fits.
ALTER TYPE "AuditAction" ADD VALUE 'REVENUE_EXPORTED';
ALTER TYPE "AuditEntityType" ADD VALUE 'PAYMENT';
