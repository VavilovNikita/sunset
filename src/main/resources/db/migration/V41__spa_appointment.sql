-- A half-hour-grid spa treatment: occupies both a POS table (in the SPA zone) and a therapist
-- for [date+startTime, date+startTime+durationMinutes). Always names a booking - hotel guests
-- only, no walk-in path, no separate client record.
--
-- date/startTime are plain `date`/`time` (no time zone anywhere) - the hotel is one place and
-- nothing else in this system converts zones (see CLAUDE.md's Dates section). The appointment's
-- end is never stored as its own column - it's always date+startTime+durationMinutes, computed
-- here in the exclusion constraints below and in application code wherever it's needed, so there
-- is exactly one source of truth for "how long" (durationMinutes) rather than two that could
-- drift apart.
CREATE TYPE "SpaAppointmentStatus" AS ENUM ('BOOKED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');

CREATE TABLE "SpaAppointment" (
    id                     text PRIMARY KEY,
    "bookingId"            text NOT NULL REFERENCES "Booking"(id),
    "tableId"              text NOT NULL REFERENCES "PosTable"(id),
    "therapistUserId"      text NOT NULL REFERENCES "User"(id),
    "treatmentMenuItemId"  text NOT NULL REFERENCES "MenuItem"(id),
    "date"                 date NOT NULL,
    "startTime"            time NOT NULL,
    -- Frozen at creation from MenuItem.durationMinutes - see the class javadoc on the entity for
    -- why this is a copy, not a live read (same "agreed terms are frozen" precedent as
    -- BookingSegmentNightlyRate).
    "durationMinutes"      integer NOT NULL CHECK ("durationMinutes" > 0),
    status                 "SpaAppointmentStatus" NOT NULL DEFAULT 'BOOKED',
    -- The POS order that charged this treatment - nullable and set independently of creation
    -- (via OrderCreateInput.spaAppointmentId). The appointment itself never computes or stores
    -- an amount; this is a pointer, not a price. A COMPLETED row with this still null is a real,
    -- visible gap the spa screen surfaces - see SpaAppointment's own openapi.yaml description.
    "orderId"              text REFERENCES "Order"(id),
    "createdByUserId"      text NOT NULL REFERENCES "User"(id),
    "cancelledByUserId"    text REFERENCES "User"(id),
    "cancelReason"         text,
    "createdAt"            timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"            timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "SpaAppointment_bookingId_idx" ON "SpaAppointment" ("bookingId");
-- The grid's own query: everything on one date.
CREATE INDEX "SpaAppointment_date_idx" ON "SpaAppointment" ("date");
-- Used to warn (not block) when a therapist is deactivated or loses the THERAPIST function
-- while holding future BOOKED appointments - see UserService.
CREATE INDEX "SpaAppointment_therapistUserId_idx" ON "SpaAppointment" ("therapistUserId");

-- Double-booking guard for both scarce resources (table AND therapist), enforced by the database
-- rather than a check-then-write in the service layer - see CLAUDE.md's Concurrency section
-- ("where a race is better solved by the database, use the database"). A plain unique constraint
-- can't express "no two overlapping time ranges" (that rule already exists for the single-point
-- cases - Payment_orderId_key, Shift_one_open_per_user); a range needs the range-native
-- equivalent, a GiST exclusion constraint. Restricted to status = 'BOOKED' so a cancelled/
-- no-show/completed appointment never blocks a new booking of the same slot - same "only an
-- active row counts" idea as Shift_one_open_per_user's own WHERE clause.
--
-- btree_gist supplies the GiST operator class needed to combine a plain equality column
-- (tableId/therapistUserId) with a range column (the computed tsrange) in one index - without it
-- only range-typed columns could participate in a GiST exclusion constraint. Marked "trusted"
-- since PostgreSQL 13, so CREATE EXTENSION only needs CREATE privilege on this database, not
-- superuser - IF NOT EXISTS makes this a safe no-op if it's already installed (see the deploy
-- notes this migration's PR/commit points at for how this was verified against the production
-- role before shipping).
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_table_overlap
  EXCLUDE USING gist (
    "tableId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status = 'BOOKED');

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_therapist_overlap
  EXCLUDE USING gist (
    "therapistUserId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status = 'BOOKED');
