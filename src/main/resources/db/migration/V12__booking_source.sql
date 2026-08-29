-- Distinguishes a public guest-submitted booking (POST /bookings, no auth) from one a staff
-- member entered directly at the front desk (POST /bookings/staff). Existing rows all predate
-- the staff endpoint, so they backfill correctly as PUBLIC.
--
-- This lets the stale-booking expiry sweep (BookingExpiryService) auto-cancel abandoned public
-- inquiries that were never confirmed, without ever touching a booking a human at the front desk
-- already made - a staff booking is confirmed by definition, by the person who created it.
CREATE TYPE "BookingSource" AS ENUM ('PUBLIC', 'STAFF');
ALTER TABLE "Booking" ADD COLUMN "source" "BookingSource" NOT NULL DEFAULT 'PUBLIC';
