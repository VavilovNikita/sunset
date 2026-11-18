-- The booking tracker (GET /reports/booking-tracker): the hotel's Excel workbook on real data. Additive only.

-- The service-charge rate the tracker takes out of room prices (which include it). For that analysis only - nothing
-- is charged. Singleton like "VatSettings" (V118): id pinned to 1 by a CHECK, seeded here, only ever updated.
CREATE TABLE "ServiceChargeSettings" (
    id                   integer PRIMARY KEY CHECK (id = 1),
    "serviceChargeRate"  numeric(5, 2) NOT NULL DEFAULT 10.00 CHECK ("serviceChargeRate" >= 0 AND "serviceChargeRate" < 100),
    "updatedAt"          timestamp(3) NOT NULL DEFAULT now()
);
INSERT INTO "ServiceChargeSettings" (id) VALUES (1);

-- Breakfast the front desk records on a booking (per night): both empty = no breakfast. The rate set mirrors the
-- BreakfastRate enum in openapi.yaml. bookedAt is SiteMinder's own "booked on" instant (UTC wall-clock like every
-- timestamp column); old SiteMinder bookings keep NULL and the tracker falls back to createdAt.
ALTER TABLE "Booking"
    ADD COLUMN "breakfastGuests" integer CHECK ("breakfastGuests" IS NULL OR "breakfastGuests" >= 0),
    ADD COLUMN "breakfastRate"   integer CHECK ("breakfastRate" IS NULL OR "breakfastRate" IN (250, 350)),
    ADD COLUMN "bookedAt"        timestamp(3);

-- Entered inputs per stay month (YYYY-MM). A row exists only while at least one of the three is set.
CREATE TABLE "BookingTrackerMonth" (
    "month"               text PRIMARY KEY CHECK ("month" ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    "targetRoomNights"    integer CHECK ("targetRoomNights" >= 0),
    "baselineRoomNights"  integer CHECK ("baselineRoomNights" >= 0),
    "baselineNetRevenue"  numeric(14, 2) CHECK ("baselineNetRevenue" >= 0),
    "updatedAt"           timestamp(3) NOT NULL DEFAULT now()
);

-- One frozen reading per Friday per stay month. (snapshotDate, stayMonth) is unique, so a repeated or racing run, or a
-- catch-up after downtime, cannot write a second one. The figures are never recomputed; only the four notes change.
CREATE TABLE "BookingTrackerSnapshot" (
    "id"               text PRIMARY KEY,
    "snapshotDate"     date NOT NULL,
    "stayMonth"        text NOT NULL CHECK ("stayMonth" ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    "late"             boolean NOT NULL DEFAULT false,
    "roomNights"       integer NOT NULL,
    "netRoomRevenue"   numeric(14, 2) NOT NULL,
    "occupancyPercent" numeric(6, 2),
    "reason"           text,
    "nextAction"       text,
    "owner"            text,
    "dueDate"          date,
    "createdAt"        timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt"        timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE ("snapshotDate", "stayMonth")
);
