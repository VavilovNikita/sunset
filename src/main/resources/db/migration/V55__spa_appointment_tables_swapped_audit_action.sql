-- New AuditAction value for POST /spa-appointments/{id}/swap-table - a distinct action from
-- SPA_APPOINTMENT_RESCHEDULED, same reasoning V49 gave for BOOKING_ROOMS_SWAPPED getting its own
-- value instead of reusing BOOKING_SEGMENT_ROOM_CHANGED: a swap moves two appointments at once,
-- for a different reason (trading resources) than an ordinary drag-to-reschedule.
ALTER TYPE "AuditAction" ADD VALUE 'SPA_APPOINTMENT_TABLES_SWAPPED';
