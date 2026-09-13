-- Never edited, only superseded - the same "agreed terms are frozen" shape as ShiftCode and
-- BookingSegmentNightlyRate, so a raise partway through a month prices each day against
-- whichever rate was actually in effect that day, not whatever the field says today.
CREATE TABLE "EmployeePayRate" (
    id                 text PRIMARY KEY,
    "employeeUserId"   text NOT NULL REFERENCES "User"(id),
    "dailyRate"        numeric(10,2) NOT NULL CHECK ("dailyRate" >= 0),
    "effectiveFrom"    date NOT NULL,
    "createdByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "EmployeePayRate_employeeUserId_effectiveFrom_key" ON "EmployeePayRate" ("employeeUserId", "effectiveFrom");
