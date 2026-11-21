-- Staff notes on a booking (airport pickup, allergies, special requests). Additive, nullable. The 1000-character cap
-- mirrors the API; blank is stored as NULL by the service.
ALTER TABLE "Booking" ADD COLUMN "notes" text CHECK ("notes" IS NULL OR char_length("notes") <= 1000);
