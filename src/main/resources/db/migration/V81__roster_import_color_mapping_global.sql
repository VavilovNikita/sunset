-- Drops staffArea from RosterImportShiftColorMapping's key - it never belonged there. The actual
-- ShiftCode a code text resolves to is already area-scoped, separately, by ShiftCodeService
-- #resolveActive(staffArea, code) at read time; what this table remembers is only "which code
-- text does this colour mean", a fact the hotel's own legend (and the accountant who set the
-- codes up) treats as the same everywhere - exactly two "9" entries, not one pair per department.
-- Keying the memory by area as well made an admin re-resolve the same two colours once per
-- department for no reason, and the first time a new department used "9" it would ask again for a
-- colour that was already answered.
--
-- If more than one area had separately recorded a mapping for the same (rawCode, fillColor) -
-- possible only because the old per-area key allowed it, and unlikely at this feature's age -
-- this keeps whichever is newest (ties broken by id) rather than leaving a conflict the new
-- unique index below can't accept. Nothing becomes unreachable: every surviving row is still
-- findable, now by the key the code actually looks it up with.
DELETE FROM "RosterImportShiftColorMapping" a
USING "RosterImportShiftColorMapping" b
WHERE a."rawCode" = b."rawCode"
  AND a."fillColor" = b."fillColor"
  AND (a."createdAt" < b."createdAt" OR (a."createdAt" = b."createdAt" AND a.id < b.id));

DROP INDEX "RosterImportShiftColorMapping_area_code_fill_key";
ALTER TABLE "RosterImportShiftColorMapping" DROP COLUMN "staffArea";
CREATE UNIQUE INDEX "RosterImportShiftColorMapping_code_fill_key" ON "RosterImportShiftColorMapping" ("rawCode", "fillColor");
