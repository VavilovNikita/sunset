-- Double-entry general ledger: chart of accounts, journal entries, journal lines. Read by
-- GET /reports/trial-balance (Z120) and GET /ledger/entries; written only by LedgerService.
--
-- No backfill, deliberately: only transactions from the moment this migration runs are posted.
-- Historical bookings/orders are never retroactively journalled - reconstructing which were
-- settled when, at which VAT rate, would be fabricating accounting history. The trial balance
-- is complete from go-live forward only (its openapi description says so).
--
-- Immutability: nothing in the app updates or deletes a JournalEntry/JournalLine (the entities
-- are @Immutable, there is no edit/delete endpoint). A mistake is corrected by a reversing entry
-- ("reversesEntryId"), unique so an entry can be reversed at most once - a unique constraint,
-- not a check-then-write, per CLAUDE.md's Concurrency section.
--
-- Balance (debits = credits per entry) is enforced in LedgerService before insert, not here: it
-- is a property of a set of rows, which a row-level CHECK cannot see.

CREATE TYPE "LedgerAccountType" AS ENUM ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE');
CREATE TYPE "JournalSourceType" AS ENUM ('BOOKING', 'POS_ORDER', 'FOLIO_PAYMENT', 'MANUAL');

-- The code is the key: it is what people type and read on a trial balance, and it never changes
-- (an account is never renamed, retyped or deleted - its type decides the side its balance sits
-- on, so changing it under existing entries would flip every figure already reported).
CREATE TABLE "LedgerAccount" (
    code        text PRIMARY KEY CHECK (code ~ '^[0-9A-Z][0-9A-Z-]{0,19}$'),
    name        text NOT NULL,
    type        "LedgerAccountType" NOT NULL,
    "createdAt" timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The seven accounts automatic posting uses (LedgerService's constants), plus two starter
-- accounts for manual entries; anything else an ADMIN adds via POST /ledger/accounts. One
-- Cash/Bank account, not one per method: a booking's room settlement (status PAID) records no
-- payment method at all, so a Cash/Card split could not be kept for room revenue.
INSERT INTO "LedgerAccount" (code, name, type) VALUES
    ('1000', 'Cash/Bank', 'ASSET'),
    ('1100', 'Guest Ledger (room charges receivable)', 'ASSET'),
    ('2100', 'VAT Payable', 'LIABILITY'),
    ('3000', 'Owner Equity', 'EQUITY'),
    ('4000', 'Room Revenue', 'REVENUE'),
    ('4100', 'F&B Revenue', 'REVENUE'),
    ('4200', 'SPA Revenue', 'REVENUE'),
    ('6000', 'General Expense', 'EXPENSE');

CREATE TABLE "JournalEntry" (
    id                text PRIMARY KEY,
    "entryDate"       date NOT NULL,
    description       text NOT NULL,
    "sourceType"      "JournalSourceType" NOT NULL,
    "sourceId"        text,
    "reversesEntryId" text UNIQUE REFERENCES "JournalEntry"(id),
    -- No FK, same as "AuditLog"."actorUserId": a record of who posted it outlives the account.
    "createdByUserId" text,
    "createdAt"       timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (("sourceType" = 'MANUAL') = ("sourceId" IS NULL))
);

CREATE INDEX "JournalEntry_entryDate_idx" ON "JournalEntry" ("entryDate");
CREATE INDEX "JournalEntry_source_idx" ON "JournalEntry" ("sourceType", "sourceId");

CREATE TABLE "JournalLine" (
    id            text PRIMARY KEY,
    "entryId"     text NOT NULL REFERENCES "JournalEntry"(id),
    "lineOrder"   integer NOT NULL,
    "accountCode" text NOT NULL REFERENCES "LedgerAccount"(code),
    debit         numeric(12, 2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
    credit        numeric(12, 2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
    -- Exactly one side non-zero.
    CHECK ((debit > 0) <> (credit > 0)),
    UNIQUE ("entryId", "lineOrder")
);

CREATE INDEX "JournalLine_accountCode_idx" ON "JournalLine" ("accountCode");
