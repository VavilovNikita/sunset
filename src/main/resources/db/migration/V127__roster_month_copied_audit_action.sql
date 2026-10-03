-- POST /roster/copy's audit entry. Alone in its file: a freshly added enum value can't be used in
-- the same transaction (see CLAUDE.md, "Migrations").
ALTER TYPE "AuditAction" ADD VALUE 'ROSTER_MONTH_COPIED';
