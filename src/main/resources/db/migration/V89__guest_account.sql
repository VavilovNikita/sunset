-- A guest's own persistent, self-service login - entirely separate from "User" (staff) and from
-- "Guest" (the staff-managed CRM contact card). See GuestAccount's own openapi.yaml description
-- and CLAUDE.md's Authorization section for why this is a brand-new identity system rather than
-- an extension of either.
--
-- email is stored lowercased (GuestAccountService is the only writer) and uniquely indexed as
-- stored - a plain Postgres unique index is enough because there's no mixed-case data to
-- normalize around, unlike "User"."email" (see V68's own comment on that column).
--
-- emailVerifiedAt null means unverified/unusable - GuestJwtAuthFilter and the login path both
-- treat null the same way JwtAuthFilter treats "User"."isActive" = false. emailVerificationToken
-- and its expiry are nullable together and cleared on successful verify - only one pending token
-- makes sense at a time (a fresh register/resend simply overwrites both), so no separate token
-- table is needed.
--
-- tokenVersion is the same revocation mechanism as "User"."tokenVersion" (see JwtAuthFilter) -
-- bumped on a self-service password change, checked by GuestJwtAuthFilter on every request.
CREATE TABLE "GuestAccount" (
    id                              text PRIMARY KEY,
    email                           text NOT NULL,
    "passwordHash"                  text NOT NULL,
    name                            text,
    "emailVerifiedAt"               timestamp(3),
    "emailVerificationToken"        text,
    "emailVerificationExpiresAt"    timestamp(3),
    "tokenVersion"                  integer NOT NULL DEFAULT 0,
    "createdAt"                     timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"                     timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "GuestAccount_email_key" ON "GuestAccount" (email);
