-- POST /night-audit/close - one row per hotel-local date a person reviewed and closed. A receipt,
-- nothing more: no other table reads it, and nothing is blocked or locked by a date having one.
-- There is no ledger here to close - a folio balance is computed live (BookingService#computeFolio).
--
-- "date" is unique: re-closing a date isn't meaningful, and the unique constraint (not a
-- check-then-write) is what makes a concurrent second close a 409 - see CLAUDE.md's Concurrency
-- section.
CREATE TABLE "NightAudit" (
    id                  text PRIMARY KEY,
    date                date NOT NULL,
    "closedByUserId"    text NOT NULL REFERENCES "User"(id),
    "closedAt"          timestamp(3) NOT NULL DEFAULT now(),
    notes               text,
    CONSTRAINT "NightAudit_date_key" UNIQUE (date)
);
