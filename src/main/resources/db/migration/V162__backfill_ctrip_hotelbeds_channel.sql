-- SiteMinder bookings whose own channel name is Ctrip / Trip.com or Hotelbeds get the new channel values (V159). Same
-- name matching as SiteMinderImportService#mapChannel (lower-case, letters and digits only, prefix). Only rows still
-- OTHER: a channel a person corrected by hand is left alone.
UPDATE "Booking"
   SET "channel" = 'CTRIP'::"BookingChannel"
 WHERE "source" = 'SITEMINDER' AND "channel" = 'OTHER'
   AND regexp_replace(lower("externalChannel"), '[^a-z0-9]', '', 'g') ~ '^(ctrip|tripcom)';

UPDATE "Booking"
   SET "channel" = 'HOTELBEDS'::"BookingChannel"
 WHERE "source" = 'SITEMINDER' AND "channel" = 'OTHER'
   AND regexp_replace(lower("externalChannel"), '[^a-z0-9]', '', 'g') ~ '^hotelbeds';
