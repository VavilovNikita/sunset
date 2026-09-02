-- Normalized (0..1) position of a physical room on the property map image (see
-- V29__property_map.sql for the image itself, stored separately - replacing the image must never
-- move a single RoomUnit). Both null = not placed on the map yet, the normal state right after
-- this ships. No pixel coordinates: the map's underlying image can be swapped for a different
-- size without any position drifting.
ALTER TABLE "RoomUnit" ADD COLUMN "positionX" NUMERIC(5,4);
ALTER TABLE "RoomUnit" ADD COLUMN "positionY" NUMERIC(5,4);

ALTER TABLE "RoomUnit" ADD CONSTRAINT room_unit_position_range
  CHECK (("positionX" IS NULL OR ("positionX" BETWEEN 0 AND 1))
     AND ("positionY" IS NULL OR ("positionY" BETWEEN 0 AND 1)));

-- A room is either fully placed (both coordinates) or not placed at all (both null) - never one
-- without the other.
ALTER TABLE "RoomUnit" ADD CONSTRAINT room_unit_position_pair
  CHECK (("positionX" IS NULL) = ("positionY" IS NULL));
