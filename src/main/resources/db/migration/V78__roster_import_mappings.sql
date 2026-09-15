-- Support tables for the Excel schedule importer (RosterImportService). Two independent facts
-- get remembered here so a person is never asked to resolve the same thing twice:
--
-- 1. Which account a name in the spreadsheet refers to. Names are spelled differently across
--    months and people come and go, so this is never inferred - a person maps each distinct
--    spelling once (RosterImportService#resolveName), and it's remembered here against the
--    account from then on.
-- 2. Which actual ShiftCode a "9" cell means, for a given (staffArea, fill colour). Two
--    different shifts are both written "9" in this file, told apart only by the cell's fill
--    (see ShiftCodeService's own javadoc); ShiftCode itself has no colour concept and can't have
--    two rows sharing (staffArea, code) at once (V57's own unique index), so the admin's actual
--    ShiftCode for the "other" one of the pair necessarily has a different code string that this
--    importer has no way to know ahead of time. Resolved once per (staffArea, fill), remembered
--    by the resulting ShiftCode's own code string (not its id) so a later edit that retires and
--    replaces that ShiftCode row (see V57's versioning) doesn't silently invalidate the mapping -
--    the (staffArea, code) lookup this remembered code string feeds is re-resolved fresh on every
--    import anyway.
CREATE TYPE "FillColor" AS ENUM ('YELLOW', 'BLUE');

CREATE TABLE "RosterImportNameMapping" (
    id                 text PRIMARY KEY,
    "rawName"          text NOT NULL,
    "employeeUserId"   text NOT NULL REFERENCES "User"(id),
    "createdByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "RosterImportNameMapping_rawName_key" ON "RosterImportNameMapping" ("rawName");

CREATE TABLE "RosterImportShiftColorMapping" (
    id                 text PRIMARY KEY,
    "staffArea"        "StaffArea" NOT NULL,
    "rawCode"          text NOT NULL,
    "fillColor"        "FillColor" NOT NULL,
    "resolvedCode"     text NOT NULL,
    "createdByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "RosterImportShiftColorMapping_area_code_fill_key"
    ON "RosterImportShiftColorMapping" ("staffArea", "rawCode", "fillColor");
