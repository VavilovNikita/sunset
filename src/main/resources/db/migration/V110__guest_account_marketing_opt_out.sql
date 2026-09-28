-- Opt-out for the automated lifecycle emails (pre-arrival, post-stay, win-back) - see
-- LifecycleEmailService. One switch for all three; there is no per-type opt-out.
--
-- unsubscribeToken is a stable per-account secret embedded in every lifecycle email's unsubscribe
-- link (GET /guest-auth/unsubscribe?token=). Unlike emailVerificationToken it is never cleared or
-- rotated once issued: an old email's link must keep working for as long as the guest has it.
-- New accounts get one from GuestAccountEntity itself; existing rows are backfilled here with
-- gen_random_uuid() (built into Postgres 13+, 122 random bits - the ids themselves are generated
-- app-side by Hibernate's @UuidGenerator, so there's no DB-side generator already in use to reuse).
ALTER TABLE "GuestAccount"
    ADD COLUMN "marketingEmailsOptOut" boolean NOT NULL DEFAULT false,
    ADD COLUMN "unsubscribeToken" text;

UPDATE "GuestAccount" SET "unsubscribeToken" = gen_random_uuid()::text WHERE "unsubscribeToken" IS NULL;

ALTER TABLE "GuestAccount" ALTER COLUMN "unsubscribeToken" SET NOT NULL;

CREATE UNIQUE INDEX "GuestAccount_unsubscribeToken_key" ON "GuestAccount" ("unsubscribeToken");
