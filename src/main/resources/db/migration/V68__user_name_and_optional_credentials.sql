-- A no-login staff account (see UserService#create's own comment) has a name but no email or
-- password - most of the people about to be entered by hand for the staff roster (cooks,
-- housekeepers) exist only so the roster/attendance/pay-rate records have someone to point at,
-- and never sign in. email and passwordHash go nullable together; name becomes the one
-- identifier that's never absent, replacing email as what every staff-facing screen (roster,
-- patterns, pay rates) displays and sorts by - see openapi.yaml's own description of each.

ALTER TABLE "User" ADD COLUMN name text;

-- Backfill from the local part of the email - the only identifier every existing row already
-- has - so the column can go NOT NULL below. A person can correct this by hand afterward; this
-- is a starting value, not a claim that it's the account's real name.
UPDATE "User" SET name = split_part(email, '@', 1) WHERE name IS NULL;

ALTER TABLE "User" ALTER COLUMN name SET NOT NULL;

ALTER TABLE "User" ALTER COLUMN email DROP NOT NULL;
ALTER TABLE "User" ALTER COLUMN "passwordHash" DROP NOT NULL;

-- The existing "User_email_key" unique index needs no change: a plain Postgres unique index
-- already treats every NULL as distinct from every other NULL, so any number of no-login rows
-- can coexist with a null email while two rows still can't share the same real one.
--
-- What does still need enforcing is that a row is never half-credentialed. POST /auth/login
-- looks a row up by email and then checks the submitted password against passwordHash - if a row
-- could have an email but no passwordHash, that check would compare against NULL instead of
-- failing the ordinary "wrong password" way. email and passwordHash must arrive and leave
-- together - see UserService#create/#grantCredentials, the only two places either is ever set.
ALTER TABLE "User" ADD CONSTRAINT user_credentials_paired CHECK (("email" IS NULL) = ("passwordHash" IS NULL));
