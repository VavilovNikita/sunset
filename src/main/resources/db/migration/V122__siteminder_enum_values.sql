-- One-way SiteMinder reservation import (POST /integrations/siteminder/reservations).
-- Enum values only: PostgreSQL forbids using a freshly added value in the same transaction, and
-- V123 uses 'SITEMINDER' in a CHECK constraint - see CLAUDE.md's Migrations section.
--
-- "BookingSource" was PUBLIC (the public form) / STAFF (front desk): how a booking entered this
-- system. A reservation read off SiteMinder is a third way in. It also keeps these bookings out
-- of BookingExpiryService's sweep, which only ever looks at unconfirmed PUBLIC bookings - an OTA
-- reservation is confirmed by the time it reaches us.
ALTER TYPE "BookingSource" ADD VALUE IF NOT EXISTS 'SITEMINDER';

-- The room-type mapping table's own CRUD. The import's booking writes reuse the existing
-- BOOKING_* actions (a SiteMinder cancellation is recorded exactly like a staff one); what tells
-- them apart is the actor, "SITEMINDER" - see AuditLogService.
ALTER TYPE "AuditAction" ADD VALUE 'SITEMINDER_ROOM_TYPE_MAPPING_CREATED';
ALTER TYPE "AuditAction" ADD VALUE 'SITEMINDER_ROOM_TYPE_MAPPING_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'SITEMINDER_ROOM_TYPE_MAPPING_DELETED';
ALTER TYPE "AuditEntityType" ADD VALUE 'SITEMINDER_ROOM_TYPE_MAPPING';
