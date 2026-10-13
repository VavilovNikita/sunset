-- Gym reservations: a hotel booking reserves the (single, exclusive) gym for a half-hour-grid
-- slot, and the equipment is switched on/off around it (GymAutomationService).
--
-- date/startTime are plain `date`/`time` in hotel (Asia/Bangkok) wall-clock, like SpaAppointment.
-- The end is never stored - always date + startTime + durationMinutes - so there is one source of
-- truth for "how long".
CREATE TYPE "GymReservationStatus" AS ENUM ('BOOKED', 'COMPLETED', 'CANCELLED');

-- Opening hours and the automation parameters, one row (same singleton shape as
-- LifecycleEmailSettings, V112). The defaults are the owner's: 30 minutes ahead, air conditioners
-- on cool (mode 0) at 16 degrees and the strongest fan (wind 3).
CREATE TABLE "GymHallSettings" (
    id             integer PRIMARY KEY CHECK (id = 1),
    "openTime"     time NOT NULL DEFAULT '06:00',
    "closeTime"    time NOT NULL DEFAULT '22:00',
    "leadMinutes"  integer NOT NULL DEFAULT 30 CHECK ("leadMinutes" BETWEEN 0 AND 180),
    "acMode"       integer NOT NULL DEFAULT 0 CHECK ("acMode" BETWEEN 0 AND 4),
    "acTemp"       integer NOT NULL DEFAULT 16 CHECK ("acTemp" BETWEEN 16 AND 30),
    "acWind"       integer NOT NULL DEFAULT 3 CHECK ("acWind" BETWEEN 0 AND 3),
    "updatedAt"    timestamp(3) NOT NULL DEFAULT now(),
    CHECK ("openTime" < "closeTime")
);
INSERT INTO "GymHallSettings" (id) VALUES (1);

-- Which gym devices the automation switches. Every existing and new device takes part by default.
ALTER TABLE "GymDevice" ADD COLUMN "inBookings" boolean NOT NULL DEFAULT true;

CREATE TABLE "GymReservation" (
    id                          text PRIMARY KEY,
    "bookingId"                 text NOT NULL REFERENCES "Booking"(id),
    "date"                      date NOT NULL,
    "startTime"                 time NOT NULL,
    "durationMinutes"           integer NOT NULL CHECK ("durationMinutes" BETWEEN 30 AND 120 AND "durationMinutes" % 30 = 0),
    status                      "GymReservationStatus" NOT NULL DEFAULT 'BOOKED',
    -- Exactly one creator: a staff member, or the guest's own account.
    "createdByUserId"           text REFERENCES "User"(id),
    "createdByGuestAccountId"   text REFERENCES "GuestAccount"(id),
    "cancelledAt"               timestamp(3),
    -- A label (a staff name, "guest", "system") - a snapshot, not a join.
    "cancelledBy"               text,
    "cancelReason"              text,
    -- Set by the automation once nothing is left to send for this reservation, so the minute
    -- sweep reads only the unfinished ones.
    "automationDone"            boolean NOT NULL DEFAULT false,
    "createdAt"                 timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"                 timestamp(3) NOT NULL DEFAULT now(),
    CHECK (num_nonnulls("createdByUserId", "createdByGuestAccountId") = 1),
    -- The grid is absolute (:00 and :30), whatever the opening hours are.
    CHECK (extract(minute FROM "startTime")::int % 30 = 0 AND extract(second FROM "startTime") = 0)
);

CREATE INDEX "GymReservation_bookingId_idx" ON "GymReservation" ("bookingId");
CREATE INDEX "GymReservation_date_idx" ON "GymReservation" ("date");
CREATE INDEX "GymReservation_automation_idx" ON "GymReservation" ("date") WHERE NOT "automationDone";

-- The gym is exclusive: at any moment at most one reservation. A range-only exclusion constraint
-- (no equality column), enforced by the database like the spa's - SpaAppointment, V41 - so a race
-- between two bookers has exactly one winner. BOOKED and COMPLETED both hold the slot; only a
-- cancelled reservation frees it. No DEFERRABLE: a move is a single-row UPDATE.
ALTER TABLE "GymReservation" ADD CONSTRAINT gym_reservation_no_overlap
  EXCLUDE USING gist (
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status IN ('BOOKED', 'COMPLETED'));

-- What the automation sent, per reservation, device and phase. This is its memory: the minute
-- sweep sends only what has no OK/SKIPPED row yet, so a restart never repeats a command and a
-- missed transition is picked up from here. The unique index is also the guard against two runs
-- creating the same row.
CREATE TABLE "GymReservationAction" (
    id                text PRIMARY KEY,
    "reservationId"   text NOT NULL REFERENCES "GymReservation"(id) ON DELETE CASCADE,
    -- Set null when the device is later removed from settings; the name below is the snapshot.
    "deviceId"        text REFERENCES "GymDevice"(id) ON DELETE SET NULL,
    "deviceName"      text NOT NULL,
    phase             text NOT NULL CHECK (phase IN ('ON', 'OFF')),
    status            text NOT NULL CHECK (status IN ('PENDING', 'OK', 'FAILED', 'SKIPPED')),
    -- Why nothing was sent: COVERED_BY_PREVIOUS, NEXT_BOOKING_SOON, MISSED, CANCELLED, DEVICE_REMOVED.
    "skipReason"      text,
    attempts          integer NOT NULL DEFAULT 0,
    "lastError"       text,
    -- UTC wall-clock like every timestamp column.
    "lastAttemptAt"   timestamp(3),
    "completedAt"     timestamp(3),
    "createdAt"       timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "GymReservationAction_unique" ON "GymReservationAction" ("reservationId", "deviceId", phase);
CREATE INDEX "GymReservationAction_reservationId_idx" ON "GymReservationAction" ("reservationId");
