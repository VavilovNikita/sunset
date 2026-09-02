-- Records that money was actually collected against a booking's folio - see BookingService's
-- computeFolioBreakdown/computeOutstandingBalance javadoc for the bug this fixes: without this
-- table, a room-charge Payment (money charged to the room, meant to be collected later) could
-- never be marked collected, so any booking that ever had one stayed flagged as owing money
-- forever - the checkout warning fired on every checkout, and RoomChargeDebtBadge never went
-- dark, regardless of what was actually paid.
--
-- Deliberately NOT tied to a Shift (unlike "Payment", which requires one for cash-drawer
-- reconciliation): this mirrors Booking.status=PAID, the existing precedent for "the room
-- portion is settled" - that flag has never had a payment record or shift linkage either. Cash
-- collected as a folio payment is not counted in end-of-shift reconciliation; giving it that
-- would be a real payment-collection feature, not this fix.
--
-- Deliberately a separate table from "Payment", not a loosened orderId on it: Payment.orderId
-- is NOT NULL because every existing Payment closes one specific POS order (and shift
-- reports/Z-reports rely on that), and a folio payment isn't tied to any order at all.
CREATE TYPE "FolioPaymentMethod" AS ENUM ('CASH', 'CARD', 'OTHER');

CREATE TABLE "FolioPayment" (
    id                  text PRIMARY KEY,
    "bookingId"         text NOT NULL REFERENCES "Booking"(id),
    method              "FolioPaymentMethod" NOT NULL,
    amount              numeric(10,2) NOT NULL,
    "recordedByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"         timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "FolioPayment_bookingId_idx" ON "FolioPayment" ("bookingId");

-- No backfill: this system has no record of which historical room-charge Payments were ever
-- actually collected at the desk - inventing settlement rows for them would be fabricating
-- history, not reconstructing a known total. Any booking already showing a debt keeps showing
-- it until staff either collect it for real or record a retroactive FolioPayment acknowledging
-- it was already handled before this table existed.
