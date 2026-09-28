-- Expedia as a booking channel. ALTER TYPE ... ADD VALUE on its own: PostgreSQL forbids using a
-- freshly added enum value in the same transaction, and Flyway wraps each migration in one. Nothing
-- here backfills EXPEDIA - no existing booking is known to have come from Expedia; staff correct a
-- booking's channel with PATCH /bookings/{id}.
ALTER TYPE "BookingChannel" ADD VALUE IF NOT EXISTS 'EXPEDIA';
