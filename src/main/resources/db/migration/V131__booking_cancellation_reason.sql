-- The staff-given reason a booking was cancelled, shown on the booking itself (CASHIER+ reads it
-- there; until now it lived only in the BOOKING_STATUS_CHANGED audit summary, which is MANAGER+).
-- Nullable and additive: set by BookingService#updateStatus on a move into CANCELLED, cleared on a
-- move out of it. Not backfilled from the audit log - existing cancellations read null, the audit
-- entry stays their record. Pure ADD COLUMN, so a code rollback leaves it harmlessly unused.
ALTER TABLE "Booking" ADD COLUMN "cancellationReason" text;
