-- Ctrip (Trip.com), Hotelbeds and a travel agent as booking channels, for the booking tracker's channel summary.
-- ALTER TYPE ... ADD VALUE on its own: PostgreSQL forbids using a freshly added enum value in the same transaction,
-- and Flyway wraps each migration in one. The backfill of existing SiteMinder bookings is V162.
ALTER TYPE "BookingChannel" ADD VALUE IF NOT EXISTS 'CTRIP';
ALTER TYPE "BookingChannel" ADD VALUE IF NOT EXISTS 'HOTELBEDS';
ALTER TYPE "BookingChannel" ADD VALUE IF NOT EXISTS 'AGENT';
