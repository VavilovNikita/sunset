-- The ZK protocol's own attendance-record layout (8/16/40 bytes per record - see
-- ZkTerminalClientImpl#parseAttendanceRecords) can only be derived reliably from a FULL log read,
-- where CMD_GET_FREE_SIZES' own record count and the returned blob's total byte size divide
-- cleanly. A windowed read's returned blob is a subset, so that division no longer lands on a
-- clean 16/40/8 - the layout has to be known ahead of time instead. Detected once on a device's
-- first full read (and re-detected on every full read after, including a fallback or a manual
-- resync) and reused for every windowed read after that, rather than re-derived each time.
ALTER TABLE "AttendanceDevice" ADD COLUMN "attendanceRecordSize" integer;
