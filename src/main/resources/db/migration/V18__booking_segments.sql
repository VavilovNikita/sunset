-- Mid-stay room changes (a guest relocated to a different room, sometimes a different room
-- *type*, partway through their stay) don't fit a model where a Booking has exactly one
-- roomId/roomUnitId/checkIn/checkOut for its whole reservation. BookingSegment splits a
-- booking's stay into a contiguous, non-overlapping sequence of "room X from date A to date B"
-- legs - the invariant (segments cover [Booking.checkIn, Booking.checkOut) with no gap and no
-- overlap) is enforced in application code (BookingWriter) on every write, not just at creation.
--
-- Booking.roomId/roomUnitId/checkIn/checkOut/totalPrice are kept as-is and are NOT removed:
-- they become denormalized mirrors (checkIn/checkOut = min/max across segments, totalPrice =
-- sum, roomId/roomUnitId = the last segment's) rather than the source of truth, so every
-- existing reader of those columns (CSV export, emails, folio, audit summaries, the calendar's
-- per-room-type occupancy count) keeps working unchanged. Occupancy/availability checks and
-- pricing now run against this table instead.
--
-- roomUnitId is nullable, same as Booking.roomUnitId today - a segment can represent "this room
-- type, no physical room chosen yet" (e.g. the tail end of a stay not yet assigned). roomId is
-- NOT NULL, same as Booking.roomId - a segment always has a room *type*, even before a specific
-- unit is picked.
CREATE TABLE "BookingSegment" (
    "id"          text PRIMARY KEY,
    "bookingId"   text NOT NULL REFERENCES "Booking"(id) ON DELETE CASCADE,
    "roomId"      text NOT NULL REFERENCES "Room"(id),
    "roomUnitId"  text REFERENCES "RoomUnit"(id),
    "checkIn"     date NOT NULL,
    "checkOut"    date NOT NULL,
    "totalPrice"  numeric(10,2) NOT NULL,
    "createdAt"   timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "BookingSegment_date_range_check" CHECK ("checkIn" < "checkOut")
);

CREATE INDEX "BookingSegment_bookingId_idx" ON "BookingSegment"("bookingId");
CREATE INDEX "BookingSegment_roomId_checkIn_checkOut_idx" ON "BookingSegment"("roomId", "checkIn", "checkOut");
CREATE INDEX "BookingSegment_roomUnitId_idx" ON "BookingSegment"("roomUnitId");

-- Backfill: every existing booking becomes exactly one segment matching its current
-- roomId/roomUnitId/checkIn/checkOut/totalPrice - a booking with no history of relocation is
-- one segment, not a special case, both here and in every read path built on this table.
INSERT INTO "BookingSegment" ("id", "bookingId", "roomId", "roomUnitId", "checkIn", "checkOut", "totalPrice")
SELECT gen_random_uuid()::text, "id", "roomId", "roomUnitId", "checkIn", "checkOut", "totalPrice"
FROM "Booking";
