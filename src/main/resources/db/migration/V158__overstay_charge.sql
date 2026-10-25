-- One row per night a CHECKED_IN guest stays past the booking's check-out: the price (frozen when the night
-- accrues, like BookingSegmentNightlyRate), the ledger entry that recognised the revenue, and - if the night
-- stopped being an overstay night (the stay was extended over it, or the guest left before it began) - when it was
-- voided and the entry that reversed it. A voided row is kept: it is the record behind a reversing entry, and it
-- makes a repeated accrual an update of the same (booking, night), never a second charge.
CREATE TABLE "OverstayCharge" (
    "id"            text PRIMARY KEY,
    "bookingId"     text NOT NULL REFERENCES "Booking" ("id"),
    "date"          date NOT NULL,
    "price"         numeric(10,2) NOT NULL CHECK ("price" >= 0),
    "entryId"       text REFERENCES "JournalEntry" ("id"),
    "voidedAt"      timestamp(3),
    "voidEntryId"   text REFERENCES "JournalEntry" ("id"),
    "createdAt"     timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE ("bookingId", "date")
);
CREATE INDEX "OverstayCharge_date_idx" ON "OverstayCharge" ("date") WHERE "voidedAt" IS NULL;
