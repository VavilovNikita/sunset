-- Who raised a block was never captured - the calendar's own block segments have always shown
-- the reason and dates but nothing about who wrote them. Nullable: every block created before
-- this migration has no way to know who created it, and that's shown as such (no backfill guess).
ALTER TABLE "RoomUnitBlock" ADD COLUMN "createdByUserId" text REFERENCES "User"(id);
