-- Introduces physical rooms as first-class records. Room stays a room *type* ("Ocean View
-- Suite"); RoomUnit is now the individual sellable unit of that type ("203"). No enum values
-- are involved in this change, so unlike V2/V3 and V6/V7 there is no same-transaction
-- visibility hazard here - both tables are created fresh in one migration.

CREATE TABLE "RoomUnit" (
    id          text PRIMARY KEY,
    "roomId"    text NOT NULL REFERENCES "Room"(id),
    label       text NOT NULL,
    "isActive"  boolean NOT NULL DEFAULT true,
    "createdAt" timestamp(3) NOT NULL DEFAULT now()
);

-- Unique across the whole hotel, not just within a room type - two different room types can
-- never both claim to contain room "203".
CREATE UNIQUE INDEX "RoomUnit_label_key" ON "RoomUnit" ("label");
CREATE INDEX "RoomUnit_roomId_idx" ON "RoomUnit" ("roomId");

-- Replaces Room-level Availability.blockedCount: a block is now "this specific physical room is
-- off-sale for this date range, for this reason" instead of an abstract per-type count. Stored
-- as a date range (like Booking.checkIn/checkOut) rather than one row per blocked day - blocks
-- are naturally described by staff as one reason-scoped period ("203 under renovation, 5th-9th"),
-- and a range keeps a season-long block to a single row instead of hundreds of daily ones.
CREATE TABLE "RoomUnitBlock" (
    id           text PRIMARY KEY,
    "roomUnitId" text NOT NULL REFERENCES "RoomUnit"(id) ON DELETE CASCADE,
    "fromDate"   date NOT NULL,
    "toDate"     date NOT NULL,
    reason       text NOT NULL,
    "createdAt"  timestamp(3) NOT NULL DEFAULT now(),
    CONSTRAINT "RoomUnitBlock_date_range_check" CHECK ("fromDate" <= "toDate")
);

CREATE INDEX "RoomUnitBlock_roomUnitId_idx" ON "RoomUnitBlock" ("roomUnitId");
