-- Booking.roomUnitId: which physical room (if any) is assigned to this booking. Nullable -
-- every pre-existing Booking row gets NULL here (the column's default), since assigning a room
-- retroactively is a front-desk decision this migration has no business making; roomId (the
-- room *type*) remains the source of truth for what was actually booked either way.
ALTER TABLE "Booking" ADD COLUMN "roomUnitId" text REFERENCES "RoomUnit"(id);
CREATE INDEX "Booking_roomUnitId_idx" ON "Booking" ("roomUnitId");

-- Both legacy sources of truth are now fully replaced: quantity by count(active RoomUnit)
-- (V9), blockedCount by RoomUnitBlock (V10). Safe to drop now that every row has been migrated.
ALTER TABLE "Room" DROP COLUMN "quantity";
DROP TABLE "Availability";
