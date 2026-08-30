-- POST /bookings/staff (front-desk walk-in creation, added alongside the booking calendar grid)
-- has always treated guestEmail/guestPhone as optional at the API and service layer
-- (StaffBookingCreateInput's schema, BookingWriter.insertStaff) - a walk-in guest may simply not
-- have one to give. This column-level NOT NULL, inherited unchanged from the original Prisma-era
-- schema (V1 baseline, written before that endpoint existed), was never relaxed when the feature
-- was added, so a real walk-in booking without contact info would fail with a raw database
-- constraint violation (a 500) instead of succeeding the way the API contract already promises.
ALTER TABLE "Booking" ALTER COLUMN "guestEmail" DROP NOT NULL;
ALTER TABLE "Booking" ALTER COLUMN "guestPhone" DROP NOT NULL;
