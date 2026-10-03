-- Self-service password reset for guest accounts (POST /guest-auth/forgot-password and
-- /reset-password). Only a SHA-256 of the emailed token is stored, so a read of this table (a
-- backup, a stray query) can't be turned into a working reset link - unlike the verification
-- token, a reset token is a credential for an already-usable account. Both columns are null
-- together; a successful reset or a newer request replaces/clears them. Additive only: existing
-- rows simply have no pending reset.
ALTER TABLE "GuestAccount"
    ADD COLUMN "passwordResetTokenHash" text,
    ADD COLUMN "passwordResetExpiresAt" timestamp(3);

CREATE UNIQUE INDEX "GuestAccount_passwordResetTokenHash_key" ON "GuestAccount" ("passwordResetTokenHash");
