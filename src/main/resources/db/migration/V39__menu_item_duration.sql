-- Minutes a scheduled treatment takes - null for every ordinary food/drink item. Not restricted
-- to department = 'SPA' by a CHECK here (that value doesn't exist as of this migration - see
-- V38, applied first) - MenuService validates the pairing at the application layer instead.
ALTER TABLE "MenuItem" ADD COLUMN "durationMinutes" integer;

ALTER TABLE "MenuItem" ADD CONSTRAINT menu_item_duration_positive
  CHECK ("durationMinutes" IS NULL OR "durationMinutes" > 0);
