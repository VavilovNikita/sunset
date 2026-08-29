-- Tracks whether BookingExpiryService has already warned staff that an unconfirmed public
-- booking is about to auto-cancel, so the reminder sweep sends that nudge at most once per
-- booking instead of on every 15-minute pass.
ALTER TABLE "Booking" ADD COLUMN "expiryReminderSent" boolean NOT NULL DEFAULT false;
