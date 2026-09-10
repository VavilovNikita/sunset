-- New AuditAction values for the segment-scoped room reassignment, the two-booking room swap,
-- and spa appointment rescheduling. One migration for all three, same "several values in one
-- file" precedent as V2/V42 - each is a bare ALTER TYPE with no DML, so there's nothing that
-- needs its own transaction-boundary split (see CLAUDE.md's Migrations section for when that
-- split *is* required).
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_SEGMENT_ROOM_CHANGED';
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_ROOMS_SWAPPED';
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_RESCHEDULED';
