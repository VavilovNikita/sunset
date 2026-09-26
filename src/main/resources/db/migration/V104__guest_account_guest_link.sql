-- Links a guest's self-service login ("GuestAccount", V89) to the CRM contact card ("Guest") it
-- belongs to, so "Guest" becomes the one person record both GuestAccount and Booking point at.
-- Nullable: an account whose email matches more than one Guest card (Guest.email has no unique
-- constraint, and duplicates exist) is deliberately left unlinked rather than guessed at - see
-- GuestLinkService. Existing rows are linked separately by V105, kept apart from this schema
-- change because it's the one that touches production data.
--
-- Unique: one account belongs to at most one Guest, and vice versa. A plain unique index is
-- enough - Postgres treats NULLs as distinct, so any number of unlinked accounts can coexist.
--
-- ON DELETE SET NULL, not RESTRICT (Booking.guestId's choice): deleting a Guest card that has an
-- account but no bookings shouldn't be blocked with "has bookings on record", and an unlinked
-- account still sees its history through GuestAccountService#listBookings's email fallback.
ALTER TABLE "GuestAccount"
    ADD COLUMN "guestId" text REFERENCES "Guest"(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX "GuestAccount_guestId_key" ON "GuestAccount" ("guestId");
