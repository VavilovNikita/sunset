-- New audit actions for POST /bookings/{id}/relocate and POST /bookings/{id}/undo-relocation
-- (see V18__booking_segments.sql). No backfill needed - these values are only ever written by
-- future relocations, never queried within this same migration transaction, so (unlike
-- V2/V6/V7's CASHIER/WAITER/BAR_TICKET split) there's no "unsafe use of new value" ordering
-- concern that would require splitting this into two migrations.
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_RELOCATED';
ALTER TYPE "AuditAction" ADD VALUE 'BOOKING_RELOCATION_UNDONE';
