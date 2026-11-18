-- Audit entries of the service-charge setting and of the booking tracker's month targets and snapshot notes.
-- Nothing in this file uses the new values - see CLAUDE.md's Migrations section.
ALTER TYPE "AuditAction" ADD VALUE IF NOT EXISTS 'SERVICE_CHARGE_SETTINGS_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE IF NOT EXISTS 'BOOKING_TRACKER_TARGET_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE IF NOT EXISTS 'BOOKING_TRACKER_SNAPSHOT_UPDATED';
