-- Audit vocabulary for voiding a sent order line (see V128's OrderItemVoid). Only ever written by
-- future actions, never used within this migration's own transaction.
ALTER TYPE "AuditAction" ADD VALUE 'ORDER_ITEM_VOIDED';
