-- SiteMinder reservation import: the idempotency key on Booking, and the room-type mapping.

-- SiteMinder's own booking reference (the one shown on both its list and its card view - not the
-- per-OTA reference, which differs by channel). Keyed together with "source" rather than a
-- separate "externalSource" column: "source" already answers "how did this booking get into
-- the system", and a second column for the same question could disagree with it.
ALTER TABLE "Booking" ADD COLUMN "externalReference" text;
-- The channel name exactly as SiteMinder displayed it ("Booking.com", "Trip.com", ...). "channel"
-- holds the mapped BookingChannel, which is OTHER for anything without a dedicated value - this
-- keeps what OTHER actually was, so those bookings can be reclassified if a value is added later.
ALTER TABLE "Booking" ADD COLUMN "externalChannel" text;
-- SiteMinder's own "last changed" time for the version of the reservation last applied (the
-- latest of its booked/modified/cancelled-on timestamps), stored as UTC. An import carrying an
-- older one is skipped as stale, so a poll that reads an out-of-date view can't undo a newer
-- change.
ALTER TABLE "Booking" ADD COLUMN "externalModifiedAt" timestamp(3);

-- The idempotency key: a repeated poll of the same reservation finds this row instead of
-- creating a second booking. A unique index, not a check-then-write (CLAUDE.md, Concurrency).
CREATE UNIQUE INDEX "Booking_source_externalReference_key"
    ON "Booking" ("source", "externalReference") WHERE "externalReference" IS NOT NULL;
ALTER TABLE "Booking" ADD CONSTRAINT "Booking_externalReference_source_check"
    CHECK (("externalReference" IS NULL) = ("source" <> 'SITEMINDER'));

-- SiteMinder's OTA-facing room type name -> this system's room type. Data, not code: the names
-- can change on SiteMinder's side and someone has to fix the mapping without a deploy. Matched
-- case-insensitively (the unique index is on lower()); an unmapped name is rejected by the
-- import, never guessed. Deleting a room type deletes its mappings, after which imports for that
-- name are rejected as unmapped - the right outcome for a type that no longer exists.
CREATE TABLE "SiteMinderRoomTypeMapping" (
    id                   text PRIMARY KEY,
    "siteMinderRoomType" text NOT NULL CHECK (btrim("siteMinderRoomType") <> ''),
    "roomId"             text NOT NULL REFERENCES "Room"(id) ON DELETE CASCADE,
    "createdAt"          timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt"          timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX "SiteMinderRoomTypeMapping_name_key" ON "SiteMinderRoomTypeMapping" (lower("siteMinderRoomType"));
