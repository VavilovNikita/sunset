-- One minimum per area - the daily totals the manager reads in the spreadsheet are hotel-wide,
-- not per area, so nothing today actually warns "Sunday has nobody in the kitchen". An area
-- with no row here has no minimum and never warns - the default is silence, not zero-as-a-rule.
CREATE TABLE "StaffAreaCoverageRule" (
    "staffArea"        "StaffArea" PRIMARY KEY,
    "minimumWorking"   integer NOT NULL CHECK ("minimumWorking" >= 0),
    "updatedByUserId"  text NOT NULL REFERENCES "User"(id),
    "updatedAt"        timestamp(3) NOT NULL DEFAULT now()
);
