-- Audit entries for the ledger's manual writes (POST /ledger/entries, .../reverse,
-- POST /ledger/accounts). Automatic postings are not audited separately - the booking status
-- change / order close / folio payment that caused them already is. Nothing in this file uses
-- the new values - see CLAUDE.md's Migrations section.
ALTER TYPE "AuditAction" ADD VALUE 'LEDGER_ENTRY_POSTED';
ALTER TYPE "AuditAction" ADD VALUE 'LEDGER_ENTRY_REVERSED';
ALTER TYPE "AuditAction" ADD VALUE 'LEDGER_ACCOUNT_CREATED';
ALTER TYPE "AuditEntityType" ADD VALUE 'LEDGER_ENTRY';
ALTER TYPE "AuditEntityType" ADD VALUE 'LEDGER_ACCOUNT';
