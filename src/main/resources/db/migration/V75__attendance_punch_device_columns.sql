-- Both null together for a MANUAL punch (see V64), both set together for a SCANNER one - a
-- device terminal re-sends the same record after a network drop, a restart, or a re-read, and
-- this triple with "punchAt" is exactly what AttendanceService#ingestDevicePunch keys on to make
-- re-ingesting the same device record a no-op instead of a duplicate row. Kept as the device's own
-- reported enrollment number, not only the resolved employeeUserId: the unique triple has to
-- describe what the device actually sent, not who we currently think that number belongs to -
-- that mapping is free to change later without breaking idempotency for punches already ingested
-- under the old one.
ALTER TABLE "AttendancePunch" ADD COLUMN "deviceId" text REFERENCES "AttendanceDevice"(id);
ALTER TABLE "AttendancePunch" ADD COLUMN "enrollmentNumber" integer;

ALTER TABLE "AttendancePunch" ADD CONSTRAINT attendance_punch_scanner_needs_device
    CHECK (source <> 'SCANNER' OR ("deviceId" IS NOT NULL AND "enrollmentNumber" IS NOT NULL));

CREATE UNIQUE INDEX "AttendancePunch_device_enrollment_punchAt_key"
    ON "AttendancePunch" ("deviceId", "enrollmentNumber", "punchAt");
