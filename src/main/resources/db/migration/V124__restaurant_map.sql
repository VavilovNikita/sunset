-- The restaurant's own floor-plan background image - a single row, upserted on every upload (id
-- is always 'default'). The third copy of this shape after PropertyMap (V29) and SpaMap (V47),
-- deliberately its own table for the same reason SpaMap is: a different room, and replacing one
-- plan must never be mistaken for replacing another. Tables are placed with the existing
-- Table.positionX/positionY (V40) - a table's zone decides which plan that position is on (SPA
-- on the spa map, everything else here), so no new position columns are needed.
CREATE TABLE "RestaurantMap" (
  id TEXT PRIMARY KEY,
  "imagePath" TEXT NOT NULL,
  "updatedByUserId" TEXT NOT NULL REFERENCES "User"(id),
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
