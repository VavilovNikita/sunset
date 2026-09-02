-- New audit action for POST /bookings/{id}/reprice (see V21__booking_segment_nightly_rates.sql).
-- No backfill needed - same reasoning as V19: only ever written by future reprice calls, never
-- queried within this same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_REPRICED';
