-- Cleaning state of a physical room, deliberately separate from RoomUnitBlock (which pulls a
-- unit off sale entirely, for a reason staff write out - maintenance, renovation). A dirty room
-- is still sellable/assignable; it just needs attention before the next guest checks in. Two
-- states, not three: an "inspected" step only earns its keep once a *different* person verifies
-- another's cleaning, and there is no housekeeper role yet - the same CASHIER+ account would be
-- marking both clean and inspected, which is process theater, not a real second check. Revisit
-- if/when a housekeeping role is added.
--
-- Existing units default to CLEAN, not DIRTY: the alternative would flag every room in the
-- hotel as needing attention the moment this ships, which is noise, not signal - staff correct
-- it as rooms actually turn over from here on.
CREATE TYPE "HousekeepingStatus" AS ENUM ('DIRTY', 'CLEAN');

ALTER TABLE "RoomUnit" ADD COLUMN "housekeepingStatus" "HousekeepingStatus" NOT NULL DEFAULT 'CLEAN';
