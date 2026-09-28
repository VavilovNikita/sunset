-- One row per automated lifecycle email that actually went out - see LifecycleEmailService.
-- Written only after a successful send, so it doubles as the sweep's idempotency record and as
-- the guest card's email history (GuestDetail.emailHistory).
--
-- A log table rather than a boolean on Booking (the shape expiryReminderSent uses): a guest can
-- legitimately get several of these over time - a pre-arrival and a post-stay email per stay, and
-- a win-back email more than once, years apart.
--
-- bookingId is null for WIN_BACK (not tied to one stay). ON DELETE SET NULL there so deleting a
-- booking never loses the fact that an email went out; ON DELETE CASCADE on guestId because a
-- history nobody can open any more is not worth blocking a guest delete over (in practice a guest
-- with log rows also has bookings, which block the delete on their own).
--
-- sentAt is hotel-local (Asia/Bangkok) wall-clock, set by the app from its shared Clock - the
-- win-back window compares it against a Clock-derived cutoff. The column default is only a
-- fallback; the app always sets it.
--
-- CREATE TYPE and using it in the same file is fine - the separate-file rule is about ADD VALUE.
CREATE TYPE "LifecycleEmailType" AS ENUM ('PRE_ARRIVAL', 'POST_STAY', 'WIN_BACK');

CREATE TABLE "GuestEmailLog" (
    id              text PRIMARY KEY,
    "guestId"       text NOT NULL REFERENCES "Guest"(id) ON DELETE CASCADE,
    "bookingId"     text REFERENCES "Booking"(id) ON DELETE SET NULL,
    type            "LifecycleEmailType" NOT NULL,
    subject         text NOT NULL,
    "sentAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "GuestEmailLog_guestId_sentAt_idx" ON "GuestEmailLog" ("guestId", "sentAt" DESC);

-- At most one per-stay email of each type per booking. The sweep already checks before sending;
-- this is the database-side backstop for that "at most one" rule (see CLAUDE.md's Concurrency
-- section - a unique constraint, not only a check-then-write).
CREATE UNIQUE INDEX "GuestEmailLog_guest_booking_type_key" ON "GuestEmailLog" ("guestId", "bookingId", type)
    WHERE "bookingId" IS NOT NULL;
