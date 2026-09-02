-- New audit actions for check-in/check-out/no-show (see V23__booking_occupancy.sql) and
-- housekeeping status changes (see V24__room_unit_housekeeping_status.sql). No backfill needed -
-- same reasoning as V19/V22: only ever written by future actions, never queried within this
-- same migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_CHECKED_IN';
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_CHECKED_OUT';
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_NO_SHOW_MARKED';
ALTER TYPE "AuditAction" ADD VALUE 'ROOM_UNIT_HOUSEKEEPING_CHANGED';
