-- New audit actions for the property map (see V28/V29). No backfill needed - same reasoning as
-- V19/V22/V25/V27: only ever written by future actions, never queried within this same migration
-- transaction.
ALTER TYPE "AuditAction" ADD VALUE 'PROPERTY_MAP_IMAGE_UPDATED';
ALTER TYPE "AuditAction" ADD VALUE 'ROOM_UNIT_POSITION_UPDATED';

-- PROPERTY_MAP_IMAGE_UPDATED needs an AuditEntityType to file under - the PropertyMap singleton
-- row's own id ('default') is used as entityId.
ALTER TYPE "AuditEntityType" ADD VALUE 'PROPERTY_MAP';
