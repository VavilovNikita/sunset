-- The spa's own floor-plan background image - a single row, upserted on every upload (id is
-- always the literal string 'default'). Its own table, deliberately not a reuse of PropertyMap
-- (V29): the spa's floor plan is a different physical layout at a different scale than the
-- property map's rooms, placed with SPA-zone Table rows (Table.positionX/positionY, V40), not
-- RoomUnit rows - a manager replacing one image must never be mistaken for replacing the other.
CREATE TABLE "SpaMap" (
  id TEXT PRIMARY KEY,
  "imagePath" TEXT NOT NULL,
  "updatedByUserId" TEXT NOT NULL REFERENCES "User"(id),
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
