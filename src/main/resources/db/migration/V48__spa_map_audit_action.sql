-- New audit action for the spa map (see V47). No backfill needed - same reasoning as
-- V19/V22/V25/V27/V30: only ever written by future actions, never queried within this same
-- migration transaction.
ALTER TYPE "AuditAction" ADD VALUE 'SPA_MAP_IMAGE_UPDATED';

-- SPA_MAP_IMAGE_UPDATED needs an AuditEntityType to file under - the SpaMap singleton row's own
-- id ('default') is used as entityId, same convention as PROPERTY_MAP.
ALTER TYPE "AuditEntityType" ADD VALUE 'SPA_MAP';
