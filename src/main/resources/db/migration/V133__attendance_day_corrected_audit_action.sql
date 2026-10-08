-- ALTER TYPE ... ADD VALUE lives alone in its migration: PostgreSQL won't let the same
-- transaction use a value it just added (see CLAUDE.md, Migrations). Nothing here uses it.
ALTER TYPE "AuditAction" ADD VALUE 'ATTENDANCE_DAY_CORRECTED';
