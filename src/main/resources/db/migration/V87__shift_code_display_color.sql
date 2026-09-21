-- A presentation attribute, not an agreed term - the second deliberate exception to "a ShiftCode
-- row is never edited once referenced" (see V85's own comment, which made the first exception for
-- `kind`). Nullable and not backfilled: every existing code starts unset, so nothing on the roster
-- grid looks different until an admin explicitly picks a colour via
-- PATCH /shift-codes/{id}/display-color. Named `displayColor`, not `fillColor` - `fillColor`
-- already means the Excel import's own cell-fill-colour classification
-- (RosterImportShiftColorMapping.fillColor), a different fact about a different thing.
ALTER TABLE "ShiftCode" ADD COLUMN "displayColor" text;
