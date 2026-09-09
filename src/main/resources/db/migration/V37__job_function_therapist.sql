-- Adds THERAPIST to the set of valid job functions - the exact case V33's own comment predicted:
-- a third function is one migration updating this CHECK constraint, not the ALTER TYPE /
-- separate-DML-migration dance a native Postgres enum would need (see CLAUDE.md's Migrations
-- section). A therapist is an ordinary staff User like any other, tagged with this function -
-- not a separate person table - so an account whose holder never signs in costs nothing beyond
-- the row itself.
ALTER TABLE "User" DROP CONSTRAINT user_job_functions_valid;

ALTER TABLE "User" ADD CONSTRAINT user_job_functions_valid
  CHECK ("jobFunctions" <@ ARRAY['ENGINEER', 'HOUSEKEEPER', 'THERAPIST']::text[]);
