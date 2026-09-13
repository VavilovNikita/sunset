CREATE TYPE "Weekday" AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

-- What POST /roster/generate reads to seed a month. defaultShiftCodeId is nullable - the source
-- spreadsheet's own last-day-of-month column shows several employees with no single "usual"
-- code at all, so generation simply leaves their dates blank for the manager to fill by hand in
-- that case, rather than forcing a default that doesn't exist.
CREATE TABLE "EmployeePattern" (
    "employeeUserId"     text PRIMARY KEY REFERENCES "User"(id),
    "staffArea"          "StaffArea" NOT NULL,
    "defaultShiftCodeId" text REFERENCES "ShiftCode"(id),
    "workDaysPerWeek"    integer NOT NULL CHECK ("workDaysPerWeek" BETWEEN 0 AND 7),
    "weeklyDayOff"       "Weekday" NOT NULL,
    "updatedByUserId"    text NOT NULL REFERENCES "User"(id),
    "updatedAt"          timestamp(3) NOT NULL DEFAULT now()
);
