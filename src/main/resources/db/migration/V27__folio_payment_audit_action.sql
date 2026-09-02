-- New audit action for POST /bookings/{id}/folio-payments (see V26__booking_folio_payments.sql).
-- No backfill needed - same reasoning as V19/V22/V25: only ever written by future settlements,
-- never queried within this same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_FOLIO_PAYMENT_RECORDED';
