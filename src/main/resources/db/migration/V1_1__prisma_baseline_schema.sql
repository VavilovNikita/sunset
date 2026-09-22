-- Recreates the pre-Flyway schema that V1__baseline.sql assumes already exists (see that
-- file's own comment). V1 only works when it runs against a database whose tables were
-- already created by the old Next.js app's Prisma migrations - see prisma/schema.prisma and
-- prisma/migrations/20260714045724_init/migration.sql in the sunset-beach repo's own history
-- (removed in that repo's commit 344ce3d, once this backend took over the schema). Against a
-- genuinely empty Postgres, spring.flyway.baseline-on-migrate never triggers (it only adopts a
-- non-empty schema), so Flyway runs V1 as an ordinary migration instead of skipping it - and
-- since V1 creates nothing, V2 immediately fails trying to ALTER a "Role" type that was never
-- created.
--
-- This file recreates exactly what that single Prisma migration created, and nothing more, so
-- V2 onward apply unmodified on top of it - on a brand-new database exactly as they already do
-- against every existing one. Versioned 1.1 (Flyway's default separator treats "_" in the
-- version part as ".") so it sorts between V1 and V2 without renumbering anything already
-- shipped.
--
-- Cross-checked against two independent sources that agree exactly: the recovered Prisma
-- migration.sql above, and src/test/resources/test-db-baseline.sql (a real
-- pg_dump --schema-only of a fully-migrated dev database, captured at V20 - by V20 every
-- column/type below has either reached its final form or, for the ones V4/V8-V11 go on to
-- drop, is confirmed already gone).

CREATE TYPE "Role" AS ENUM ('ADMIN', 'MANAGER');

CREATE TYPE "BookingStatus" AS ENUM ('NEW', 'CONFIRMED', 'PAID', 'CANCELLED');

CREATE TABLE "User" (
    "id"           text NOT NULL,
    "email"        text NOT NULL,
    "passwordHash" text NOT NULL,
    "role"         "Role" NOT NULL DEFAULT 'MANAGER',
    "createdAt"    timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

CREATE TABLE "Room" (
    "id"          text NOT NULL,
    "name"        text NOT NULL,
    "description" text NOT NULL,
    "capacity"    integer NOT NULL,
    "basePrice"   numeric(10,2) NOT NULL,
    "images"      text[] DEFAULT ARRAY[]::text[],
    "createdAt"   timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Room_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "RatePlan" (
    "id"        text NOT NULL,
    "roomId"    text NOT NULL,
    "date"      date NOT NULL,
    "price"     numeric(10,2) NOT NULL,
    "createdAt" timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "RatePlan_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "RatePlan_roomId_date_key" ON "RatePlan"("roomId", "date");

-- Dropped later by V11 (replaced by RoomUnitBlock) - recreated here only so V4's own ALTER
-- statements (add blockedCount, drop isBlocked) have something to apply to, exactly as they
-- did against the real Prisma-created table.
CREATE TABLE "Availability" (
    "id"        text NOT NULL,
    "roomId"    text NOT NULL,
    "date"      date NOT NULL,
    "isBlocked" boolean NOT NULL DEFAULT true,
    "createdAt" timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Availability_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "Availability_roomId_date_key" ON "Availability"("roomId", "date");

CREATE TABLE "Booking" (
    "id"          text NOT NULL,
    "roomId"      text NOT NULL,
    "guestName"   text NOT NULL,
    "guestEmail"  text NOT NULL,
    "guestPhone"  text NOT NULL,
    "checkIn"     date NOT NULL,
    "checkOut"    date NOT NULL,
    "totalPrice"  numeric(10,2) NOT NULL,
    "status"      "BookingStatus" NOT NULL DEFAULT 'NEW',
    "paymentNote" text,
    "createdAt"   timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt"   timestamp(3) NOT NULL,

    CONSTRAINT "Booking_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "Booking_roomId_checkIn_checkOut_idx" ON "Booking"("roomId", "checkIn", "checkOut");
CREATE INDEX "Booking_status_idx" ON "Booking"("status");

ALTER TABLE "RatePlan" ADD CONSTRAINT "RatePlan_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES "Room"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "Availability" ADD CONSTRAINT "Availability_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES "Room"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "Booking" ADD CONSTRAINT "Booking_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES "Room"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
