-- Normalized (0..1) position of a POS table on the spa's own floor-plan image - same shape as
-- RoomUnit.positionX/Y (V28), deliberately not shared with it: the spa's floor plan is a
-- different image from the front desk's property map, and a PosTable is not a RoomUnit.
ALTER TABLE "PosTable" ADD COLUMN "positionX" NUMERIC(5,4);
ALTER TABLE "PosTable" ADD COLUMN "positionY" NUMERIC(5,4);

ALTER TABLE "PosTable" ADD CONSTRAINT pos_table_position_range
  CHECK (("positionX" IS NULL OR ("positionX" BETWEEN 0 AND 1))
     AND ("positionY" IS NULL OR ("positionY" BETWEEN 0 AND 1)));

-- A table is either fully placed (both coordinates) or not placed at all (both null).
ALTER TABLE "PosTable" ADD CONSTRAINT pos_table_position_pair
  CHECK (("positionX" IS NULL) = ("positionY" IS NULL));
