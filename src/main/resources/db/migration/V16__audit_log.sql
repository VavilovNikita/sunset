-- Append-only record of significant staff actions - see AuditLogService's javadoc for what gets
-- written and why (never through Hibernate Envers: no shadow tables, explicit writes from the
-- service layer instead).
--
-- "actorUserId" is deliberately NOT a foreign key to "User": the whole point of also storing
-- "actorEmail"/"actorRole" as snapshots is that this row must keep saying who did something even
-- if that user's account is later renamed, or (should account deletion ever be added - today
-- accounts are only disabled, never deleted) removed entirely. A live join back to "User" would
-- silently start lying the moment that row changed.
CREATE TYPE "AuditAction" AS ENUM (
    'BOOKING_CREATED',
    'BOOKING_STATUS_CHANGED',
    'BOOKING_PAYMENT_NOTE_CHANGED',
    'BOOKING_SCHEDULE_CHANGED',
    'BOOKING_ROOM_ASSIGNED',
    'BOOKINGS_EXPORTED',
    'ROOM_PRICE_CHANGED',
    'RATE_OVERRIDE_CHANGED',
    'ORDER_CLOSED',
    'ORDER_CANCELLED',
    'ROOM_CHARGE_POSTED',
    'SHIFT_OPENED',
    'SHIFT_CLOSED',
    'SHIFT_EXPORTED',
    'USER_CREATED',
    'USER_ROLE_CHANGED',
    'USER_ACTIVE_CHANGED',
    'USER_PASSWORD_RESET',
    'ROOM_UNIT_CREATED',
    'ROOM_UNIT_UPDATED',
    'ROOM_UNIT_DELETED',
    'ROOM_UNIT_BLOCK_CREATED',
    'ROOM_UNIT_BLOCK_DELETED'
);

-- Labels here are Hibernate's default @Enumerated(EnumType.STRING) representation - the Java
-- enum constant's own name() (BOOKING, ROOM_UNIT, ...), not the PascalCase value the AuditEntityType
-- OpenAPI schema exposes over the API (Booking, RoomUnit, ...). Every other native-enum column in
-- this schema (Role, BookingStatus, ...) only gets away with skipping this distinction because
-- their JSON @JsonValue already happens to equal the Java constant name; this is the first one
-- where the two differ, so the DB labels are spelled out explicitly to match what Hibernate will
-- actually send, not what looks nicest in a JSON response.
CREATE TYPE "AuditEntityType" AS ENUM ('BOOKING', 'ROOM', 'ORDER', 'SHIFT', 'USER', 'ROOM_UNIT');

CREATE TABLE "AuditLog" (
    id            text PRIMARY KEY,
    "actorUserId" text NOT NULL,
    "actorEmail"  text NOT NULL,
    "actorRole"   "Role" NOT NULL,
    action        "AuditAction" NOT NULL,
    "entityType"  "AuditEntityType" NOT NULL,
    -- Nullable: an action with no single-record target (e.g. BOOKINGS_EXPORTED, a query over
    -- many bookings at once) has an entityType but no one entityId to point at.
    "entityId"    text,
    summary       text NOT NULL,
    "createdAt"   timestamp(3) NOT NULL DEFAULT now()
);

-- One index per GET /audit-log filter dimension (actor, action, entity, date range), plus the
-- composite (entityType, entityId) a single record's own history view (e.g. the booking detail
-- page) queries by directly.
CREATE INDEX "AuditLog_actorUserId_idx" ON "AuditLog" ("actorUserId");
CREATE INDEX "AuditLog_action_idx" ON "AuditLog" ("action");
CREATE INDEX "AuditLog_entityType_entityId_idx" ON "AuditLog" ("entityType", "entityId");
CREATE INDEX "AuditLog_createdAt_idx" ON "AuditLog" ("createdAt");
