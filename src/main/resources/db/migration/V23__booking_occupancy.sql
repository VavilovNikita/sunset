-- Physical occupancy - "is the guest actually here" - as its own axis, deliberately separate
-- from Booking.status (which stays purely commercial: confirmed, paid, cancelled). Overloading
-- status with "checked in"/"checked out" would repeat the exact mistake PAID already made
-- (it means both "the room is paid" and, informally, "everything about this booking is done" -
-- which is why RoomChargeDebtBadge had to exist as a bolt-on correction). One value per booking,
-- not per segment: a guest checks in once and checks out once regardless of how many times
-- they're relocated to a different room mid-stay.
--
-- NO_SHOW is a label, not an action - it does not touch checkIn/checkOut/status and does not
-- free any nights. Occupancy must never feed back into availability (that engine reads
-- BookingSegment/Booking.status only, and stays that way); the deliberate step to actually
-- release a no-show's remaining nights is the existing cancel/shorten path, not this column.
CREATE TYPE "OccupancyStatus" AS ENUM ('EXPECTED', 'CHECKED_IN', 'CHECKED_OUT', 'NO_SHOW');

ALTER TABLE "Booking" ADD COLUMN "occupancyStatus" "OccupancyStatus" NOT NULL DEFAULT 'EXPECTED';
ALTER TABLE "Booking" ADD COLUMN "checkedInAt" timestamp(3);
ALTER TABLE "Booking" ADD COLUMN "checkedOutAt" timestamp(3);

-- No backfill needed beyond the column default: every existing booking becomes EXPECTED with no
-- recorded check-in/out, an honest "we don't know" rather than a guess at history this system
-- never tracked before now.
