-- A system-initiated audit action (BookingExpiryService's scheduled sweep, which runs with no
-- authenticated staff principal to read a role from) has no real Role to snapshot. actorUserId
-- and actorEmail stay NOT NULL and get fixed sentinel literals ("SYSTEM" /
-- "system@sunsetbeach.internal") instead - only actorRole is blocked from doing the same because
-- it's a native "Role" enum, not free text. See AuditLogService#recordSystemAction.
ALTER TABLE "AuditLog" ALTER COLUMN "actorRole" DROP NOT NULL;
