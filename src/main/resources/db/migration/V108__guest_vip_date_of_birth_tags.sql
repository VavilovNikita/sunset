-- Three staff-entered facts about a guest: a VIP flag, a date of birth, and free-form tags.
-- Purely additive, exactly as the Guest schema's own openapi.yaml description anticipated.
--
-- tags is plain text[] (same shape as Room.images - see V33/V35), not a join table or an enum:
-- staff jot down whatever is useful ("honeymoon", "allergic to shellfish") and adding a new tag
-- must never need a migration. NOT NULL with an empty-array default so "no tags" has exactly one
-- representation, matching the API's "empty array, never null" contract.
--
-- The defaults backfill every existing row in the same statement: not VIP, birth date unknown,
-- no tags.
ALTER TABLE "Guest"
    ADD COLUMN "vip" boolean NOT NULL DEFAULT false,
    ADD COLUMN "dateOfBirth" date,
    ADD COLUMN "tags" text[] NOT NULL DEFAULT ARRAY[]::text[];
