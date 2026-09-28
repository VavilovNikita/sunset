-- POST /night-audit/close's audit entry. Nothing in this file uses either new value - see
-- CLAUDE.md's Migrations section: Postgres forbids using a freshly added enum value in the same
-- transaction.
ALTER TYPE "AuditAction" ADD VALUE 'NIGHT_AUDIT_CLOSED';
ALTER TYPE "AuditEntityType" ADD VALUE 'NIGHT_AUDIT';
