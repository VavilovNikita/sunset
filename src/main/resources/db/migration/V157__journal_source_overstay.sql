-- A night a guest stays past the booking's check-out is charged to the booking and posted to the ledger when it
-- accrues (V158). Its entries get their own source so that "booking left PAID: mirror its settlements" never
-- picks them up. Own file: a freshly added enum value cannot be used in the transaction that adds it.
ALTER TYPE "JournalSourceType" ADD VALUE IF NOT EXISTS 'OVERSTAY';
