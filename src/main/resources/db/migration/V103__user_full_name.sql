-- An optional full legal name, for admin reference only - separate from "name", which stays the
-- short display name shown everywhere else (POS headers, punch logs, audit entries). Nullable,
-- no backfill: nothing reads it except the admin Users screen.
ALTER TABLE "User" ADD COLUMN "fullName" TEXT;
