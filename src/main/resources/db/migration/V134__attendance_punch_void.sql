-- Attendance punches are never deleted or edited: fixing a day (PUT /attendance/day) *voids* the
-- live punches and records new ones, so the earlier record stays and can't be passed off as never
-- having existed. A voided punch counts in no report, summary, today board or direction/debounce
-- check; it still blocks re-ingestion of the same scanner record (the unique triple from V75 is
-- untouched), so a corrected scanner punch can't come back on the next poll.
--
-- "correctionId" is set on the punches a correction recorded; "voidedByCorrectionId" on the ones it
-- replaced - together they group a day's versions. The void columns are all set or all null.
-- All nullable, no backfill: every existing punch is live. Pure ADD COLUMN.
ALTER TABLE "AttendancePunch" ADD COLUMN "correctionId" text;
ALTER TABLE "AttendancePunch" ADD COLUMN "voidedAt" timestamp(3);
ALTER TABLE "AttendancePunch" ADD COLUMN "voidedByUserId" text REFERENCES "User"(id);
ALTER TABLE "AttendancePunch" ADD COLUMN "voidedByCorrectionId" text;
ALTER TABLE "AttendancePunch" ADD COLUMN "voidReason" text;

ALTER TABLE "AttendancePunch" ADD CONSTRAINT attendance_punch_void_columns_together
    CHECK (("voidedAt" IS NULL) = ("voidedByUserId" IS NULL)
       AND ("voidedAt" IS NULL) = ("voidedByCorrectionId" IS NULL)
       AND ("voidedAt" IS NULL) = ("voidReason" IS NULL));
