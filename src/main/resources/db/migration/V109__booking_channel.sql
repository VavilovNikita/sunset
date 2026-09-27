-- How a booking reached the hotel (DIRECT, PHONE, WALK_IN, BOOKING_COM, AIRBNB, AGODA, OTHER) -
-- a marketing-channel label staff record, nothing more. Deliberately a new type and a new column,
-- NOT new values on "BookingSource" (V12's PUBLIC/STAFF column): that one means "public form vs.
-- front desk" and is what BookingExpiryService's auto-cancel sweep filters on, so an OTA value
-- added there would silently move bookings into or out of that sweep.
--
-- CREATE TYPE and using its values in the same file is fine: the "ALTER TYPE ... ADD VALUE in its
-- own migration" rule is about adding a value to an existing type, not creating a new one.
CREATE TYPE "BookingChannel" AS ENUM ('DIRECT', 'PHONE', 'WALK_IN', 'BOOKING_COM', 'AIRBNB', 'AGODA', 'OTHER');

ALTER TABLE "Booking" ADD COLUMN "channel" "BookingChannel";

-- Backfill: a public-form booking is DIRECT by definition (that is also what POST /bookings sets
-- from now on). A staff-entered booking's real channel was never recorded, so it gets OTHER rather
-- than a guess - staff can correct it with PATCH /bookings/{id}.
UPDATE "Booking" SET "channel" = CASE WHEN "source" = 'PUBLIC' THEN 'DIRECT'::"BookingChannel" ELSE 'OTHER'::"BookingChannel" END;

-- No column default: every insert path sets channel explicitly (DIRECT server-side for the
-- public form, a required field for POST /bookings/staff), so a missing value is a bug to surface,
-- not something to paper over.
ALTER TABLE "Booking" ALTER COLUMN "channel" SET NOT NULL;
