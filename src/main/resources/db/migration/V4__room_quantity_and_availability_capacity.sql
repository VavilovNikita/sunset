-- Turns Room from "one physical room" into "a room type with N sellable units".
-- Availability.isBlocked (whole-room boolean stop-sell) becomes Availability.blockedCount
-- (how many units of the type are pulled from sale on that date). Booking is untouched -
-- it still references roomId and still means "one unit, one date range"; the
-- remaining-inventory arithmetic (quantity - blockedCount - active bookings) now lives in
-- the availability engine (AvailabilityService/BookingWriter), not in the schema. Physical
-- unit numbering/assignment is intentionally out of scope - this only adds a count.

ALTER TABLE "Room" ADD COLUMN "quantity" integer NOT NULL DEFAULT 1;
ALTER TABLE "Room" ADD CONSTRAINT "Room_quantity_check" CHECK ("quantity" >= 1);

ALTER TABLE "Availability" ADD COLUMN "blockedCount" integer;

-- isBlocked = true meant "this room type is off-sale for the day" - in the pre-quantity
-- model that was always all of it (quantity was implicitly 1 for every room), so it carries
-- forward as "all currently-known units blocked". isBlocked = false rows mean "not blocked",
-- i.e. 0.
UPDATE "Availability" a
SET "blockedCount" = CASE WHEN a."isBlocked" THEN r."quantity" ELSE 0 END
FROM "Room" r
WHERE r."id" = a."roomId";

-- Zero is defined as equivalent to "no manual block row" for a date going forward
-- (see AvailabilityService.setAvailability) - drop the now-redundant zero rows instead of
-- carrying them as permanent dead weight.
DELETE FROM "Availability" WHERE "blockedCount" = 0;

ALTER TABLE "Availability" ALTER COLUMN "blockedCount" SET NOT NULL;
ALTER TABLE "Availability" ADD CONSTRAINT "Availability_blockedCount_check" CHECK ("blockedCount" >= 0);
ALTER TABLE "Availability" DROP COLUMN "isBlocked";
