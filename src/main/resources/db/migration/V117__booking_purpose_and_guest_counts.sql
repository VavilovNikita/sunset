-- Why a room is occupied (STANDARD paying stay, COMPLIMENTARY, HOUSE_USE) - deliberately its own
-- type and column, independent of "BookingChannel" (how the booking reached the hotel): a comp can
-- still arrive by phone or walk-in. A house-use or comp stay is still a Booking, not a
-- RoomUnitBlock: the room is occupied and counts as room-nights sold in the reports.
--
-- CREATE TYPE and using its values in the same file is fine: the "ADD VALUE in its own migration"
-- rule is about adding a value to an existing type (see V116), not creating a new one.
CREATE TYPE "BookingPurpose" AS ENUM ('STANDARD', 'COMPLIMENTARY', 'HOUSE_USE');

-- Every existing booking was an ordinary stay as far as anything recorded - the default is the
-- backfill.
ALTER TABLE "Booking" ADD COLUMN "purpose" "BookingPurpose" NOT NULL DEFAULT 'STANDARD';

-- Party size. Bookings made before this column existed never recorded one, so they are backfilled
-- as 1 adult, 0 children - an assumption, not a fact, and nothing corrects it later. After the
-- backfill "adults" has no default: both create endpoints require it, so a missing value is a bug
-- to surface.
ALTER TABLE "Booking" ADD COLUMN "adults" INTEGER;
UPDATE "Booking" SET "adults" = 1;
ALTER TABLE "Booking" ALTER COLUMN "adults" SET NOT NULL;
ALTER TABLE "Booking" ADD COLUMN "children" INTEGER NOT NULL DEFAULT 0;

ALTER TABLE "Booking" ADD CONSTRAINT "Booking_adults_check" CHECK ("adults" >= 1);
ALTER TABLE "Booking" ADD CONSTRAINT "Booking_children_check" CHECK ("children" >= 0);
