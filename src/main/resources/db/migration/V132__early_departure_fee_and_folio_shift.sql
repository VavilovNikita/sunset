-- Two additive columns for the early-checkout / one-balance folio rework.
--
-- "Booking"."earlyDepartureFee": an early checkout can shorten the stay to the day the guest
-- actually left (releasing the room for resale) while staff choose, per checkout, to still charge
-- the nights the guest didn't use. Those nights' frozen rates are deleted with the nights
-- themselves (BookingWriter#updateSchedule), so the amount is kept here instead of inventing
-- nights that didn't happen. totalPrice stays "the nights actually stayed" for every
-- occupancy/revenue-per-night reader; the folio and the ledger settlement add this on top.
-- Zero for every existing booking, which is exactly what they were charged.
--
-- "FolioPayment"."shiftId": the open cash shift of whoever recorded the payment, so cash taken at
-- reception counts in that shift's expected cash. Nullable: a payment recorded with no open shift
-- (and every row before this migration) belongs to no drawer. No backfill - matching old rows to a
-- shift by time and user would be a guess.
--
-- Pure ADD COLUMN; a code rollback leaves both unused, but an early-departure fee written after
-- this runs would then silently drop out of the folio - restore from a dump instead.
ALTER TABLE "Booking" ADD COLUMN "earlyDepartureFee" numeric(10,2) NOT NULL DEFAULT 0;

ALTER TABLE "FolioPayment" ADD COLUMN "shiftId" text REFERENCES "Shift"(id);
CREATE INDEX "FolioPayment_shiftId_idx" ON "FolioPayment" ("shiftId");
