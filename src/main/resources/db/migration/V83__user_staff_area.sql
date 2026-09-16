-- staffArea moves from EmployeePattern onto User: it is a fact about the person (what
-- department they belong to), independent of whether they have a full weekly generation pattern
-- (workDaysPerWeek/weeklyDayOff/defaultShiftCodeId) set up yet. Before this, an account created
-- mid-roster-import had nowhere to record its department at all - a pattern is deliberately never
-- fabricated for one (see RosterImportService's own comment: workDaysPerWeek/weeklyDayOff can't
-- be reliably inferred from one month), which meant every imported employee sat under "no area
-- set" and could never be counted toward a coverage minimum.
--
-- Backfilled from the existing EmployeePattern rows first, so nobody who already had a pattern
-- loses their area; the column is then dropped from EmployeePattern, which keeps only the fields
-- that make a *pattern* (workDaysPerWeek, weeklyDayOff, defaultShiftCodeId) - staffArea has
-- exactly one home now, read straight off User (see RosterService#listEmployees,
-- EmployeePatternService#set/toDto).
ALTER TABLE "User" ADD COLUMN "staffArea" "StaffArea";

UPDATE "User" u
SET "staffArea" = ep."staffArea"
FROM "EmployeePattern" ep
WHERE ep."employeeUserId" = u.id;

ALTER TABLE "EmployeePattern" DROP COLUMN "staffArea";
