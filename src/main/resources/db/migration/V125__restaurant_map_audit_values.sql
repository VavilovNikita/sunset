-- Audit vocabulary for the restaurant map (see V124), same pair V48 added for the spa map. Only
-- ever written by future actions, never used within this migration's own transaction.
ALTER TYPE "AuditAction" ADD VALUE 'RESTAURANT_MAP_IMAGE_UPDATED';
ALTER TYPE "AuditEntityType" ADD VALUE 'RESTAURANT_MAP';
