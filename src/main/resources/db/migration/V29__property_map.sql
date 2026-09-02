-- The property map's background image - a single row, upserted on every upload (id is always the
-- literal string 'default'; there is exactly one property-wide floor plan, not one per room or
-- room type). Deliberately its own table, not a column on RoomUnit or Room: replacing this image
-- must never touch RoomUnit.positionX/positionY (V28), and there is no existing Hotel/Settings
-- table in this project to hang a single "current plan" value off of.
CREATE TABLE "PropertyMap" (
  id TEXT PRIMARY KEY,
  "imagePath" TEXT NOT NULL,
  "updatedByUserId" TEXT NOT NULL REFERENCES "User"(id),
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
