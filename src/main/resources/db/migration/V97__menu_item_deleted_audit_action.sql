-- Separate migration file from anything that uses this value - see CLAUDE.md's Migrations
-- section: Postgres forbids using a freshly added enum value in the same transaction.
ALTER TYPE "AuditAction" ADD VALUE 'MENU_ITEM_DELETED';
