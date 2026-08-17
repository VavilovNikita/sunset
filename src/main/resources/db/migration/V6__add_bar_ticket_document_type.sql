-- Splits the previously-shared KITCHEN_TICKET value into KITCHEN_TICKET/BAR_TICKET, so the
-- print-job queue can be filtered by station. Kept as its own migration, separate from V7 (which
-- backfills existing rows): Postgres allows ALTER TYPE ... ADD VALUE inside a transaction, but a
-- newly added enum value can't be referenced until that transaction has committed. Flyway commits
-- each migration file as its own transaction, so splitting these into V6/V7 means the backfill is
-- always free to use BAR_TICKET without hitting "unsafe use of new value" errors - same fix as
-- V2/V3 for the CASHIER/WAITER roles.
ALTER TYPE "PrintDocumentType" ADD VALUE 'BAR_TICKET';
