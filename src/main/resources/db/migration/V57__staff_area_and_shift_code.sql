-- The staff roster module - see CLAUDE.md's Naming section (roster/attendance/shift already
-- reserved) and this migration's own choice of "StaffArea", not "Department": PrinterDepartment
-- and MenuDepartment already exist and mean ticket/print routing, unrelated to staff - reusing
-- the word here would be the same collision CLAUDE.md warns against for "Shift".
CREATE TYPE "StaffArea" AS ENUM ('ADMIN', 'FRONT_OFFICE', 'MAINTENANCE', 'HOUSEKEEPING', 'RESTAURANT', 'KITCHEN');

-- One version of one shift code in one area - never edited once a RosterEntry references it.
-- The source spreadsheet this replaces shows the same code meaning different hours in a
-- different department ("9" is a split 09:00-13:00/16:00-21:00 in Restaurant/Kitchen, a single
-- 09:00-18:00 shift in Front Office) and, separately, a code's own hours drifting between
-- months (its own "SS" was retyped with different times in June than in July) - both are why
-- every code is scoped to one area and versioned by effectiveFrom, rather than one global,
-- eternal lookup. Editing a code's hours means inserting a new row with the same
-- staffArea+code and a later effectiveFrom; nothing here is ever UPDATEd once written.
--
-- startTime1/endTime1 is the first (or only) interval; startTime2/endTime2 exists only for a
-- split shift. All four null means OP (worked, no fixed hours). countsAsWorked and isPaid are
-- independent booleans, not an enum, because PH needs isPaid=true with countsAsWorked=false -
-- paid, but not a working day for coverage or attendance purposes - which is what lets PH stay
-- one code on the grid (holiday, annual leave, or a kept day off, all covered) without the
-- ambiguity of those three meaning different things for payroll.
CREATE TABLE "ShiftCode" (
    id                 text PRIMARY KEY,
    "staffArea"        "StaffArea" NOT NULL,
    code               text NOT NULL,
    "startTime1"       time,
    "endTime1"         time,
    "startTime2"       time,
    "endTime2"         time,
    "countsAsWorked"   boolean NOT NULL,
    "isPaid"           boolean NOT NULL,
    "effectiveFrom"    date NOT NULL,
    active             boolean NOT NULL DEFAULT true,
    "createdByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"        timestamp(3) NOT NULL DEFAULT now(),
    CONSTRAINT shift_code_interval1_pair CHECK (("startTime1" IS NULL) = ("endTime1" IS NULL)),
    CONSTRAINT shift_code_interval2_pair CHECK (("startTime2" IS NULL) = ("endTime2" IS NULL)),
    CONSTRAINT shift_code_interval2_needs_interval1 CHECK ("startTime2" IS NULL OR "startTime1" IS NOT NULL),
    CONSTRAINT shift_code_interval1_order CHECK ("startTime1" IS NULL OR "endTime1" > "startTime1"),
    CONSTRAINT shift_code_interval2_order CHECK ("startTime2" IS NULL OR "endTime2" > "startTime2")
);

CREATE INDEX "ShiftCode_staffArea_code_idx" ON "ShiftCode" ("staffArea", code);

-- One (staffArea, code) may have several versions over time, but never two starting on the same
-- date - which version was "the" one on a given day would otherwise be ambiguous.
CREATE UNIQUE INDEX "ShiftCode_staffArea_code_effectiveFrom_key" ON "ShiftCode" ("staffArea", code, "effectiveFrom");
