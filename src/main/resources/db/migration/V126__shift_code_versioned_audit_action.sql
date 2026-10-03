-- Audit vocabulary for editing a shift code as a new version (POST /shift-codes/{id}/versions).
-- Only ever written by future actions, never used within this migration's own transaction.
ALTER TYPE "AuditAction" ADD VALUE 'SHIFT_CODE_VERSIONED';
