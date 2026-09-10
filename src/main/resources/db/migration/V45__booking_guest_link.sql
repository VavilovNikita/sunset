-- Booking.guestId: an optional, independent link to a Guest record (V44), set only when
-- reception links one via PUT /bookings/{id}/guest. Nullable - every pre-existing Booking row
-- gets NULL here, and Booking's own guestName/guestEmail/guestPhone snapshot stays untouched by
-- this migration either way; see Guest's own description in openapi.yaml for why the two never
-- need to agree. No ON DELETE clause: the default RESTRICT is exactly what GuestService.delete
-- relies on to block deleting a guest that still has bookings on record - see that method's own
-- comment for why this is the deliberate backstop, not the primary check.
ALTER TABLE "Booking" ADD COLUMN "guestId" text REFERENCES "Guest"(id);
CREATE INDEX "Booking_guestId_idx" ON "Booking" ("guestId");
