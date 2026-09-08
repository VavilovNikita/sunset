-- Room maintenance tasks: someone notices a problem in a physical room, files a task against it
-- (description + photos), a manager may separately pull the room off sale (RoomUnitBlock is
-- optional - a dead light bulb needs a task, not a block), and an engineer works the task
-- through OPEN -> IN_PROGRESS -> DONE. No same-transaction visibility hazard here (unlike
-- V2/V3, V6/V7): every value this enum will ever need at launch is known up front, so it's
-- created and used in the same migration, same reasoning as V24's HousekeepingStatus.
CREATE TYPE "MaintenanceTaskStatus" AS ENUM ('OPEN', 'IN_PROGRESS', 'DONE');

CREATE TABLE "MaintenanceTask" (
    id                   text PRIMARY KEY,
    "roomUnitId"         text NOT NULL REFERENCES "RoomUnit"(id),
    description          text NOT NULL,
    status               "MaintenanceTaskStatus" NOT NULL DEFAULT 'OPEN',
    -- Optional, and set independently of task creation (see POST /maintenance-tasks/{id}/block).
    -- ON DELETE SET NULL: a manager can still remove a block directly via the pre-existing
    -- DELETE /room-units/{id}/blocks/{blockId} without that failing or orphaning the task - it
    -- just reverts to "no block". The service layer re-checks the block still exists before
    -- acting on it regardless (a raw SQL delete elsewhere shouldn't be trusted to have run this
    -- trigger before the service reads its own in-memory copy of the task).
    "blockId"            text REFERENCES "RoomUnitBlock"(id) ON DELETE SET NULL,
    "reportedByUserId"   text NOT NULL REFERENCES "User"(id),
    -- Same shape as Room.images (plain text[], not a join table) - see that column's own
    -- precedent. Stores this task's own served path (`/maintenance-tasks/{id}/photos/{file}`),
    -- not a /uploads/** path - these are staff-only, never public (see ImageUploadValidator's
    -- other caller, PropertyMapService, for the same staff-only-image precedent).
    photos               text[] NOT NULL DEFAULT '{}',
    "createdAt"          timestamp(3) NOT NULL DEFAULT now(),
    -- Set when status becomes DONE - also the day used to shorten/remove a linked block, so it
    -- doubles as the block-lifting timestamp's source, not just a display field.
    "closedAt"           timestamp(3)
);

-- A block can belong to at most one task.
CREATE UNIQUE INDEX "MaintenanceTask_blockId_key" ON "MaintenanceTask" ("blockId");
CREATE INDEX "MaintenanceTask_roomUnitId_idx" ON "MaintenanceTask" ("roomUnitId");
-- The engineer's page filters to OPEN/IN_PROGRESS by default - same reasoning as Order_status_idx.
CREATE INDEX "MaintenanceTask_status_idx" ON "MaintenanceTask" ("status");
