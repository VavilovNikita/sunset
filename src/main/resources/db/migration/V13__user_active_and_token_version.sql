-- Lets a staff account be disabled and its outstanding JWTs invalidated without rotating the
-- shared signing secret (app.security.jwt-secret), which would otherwise log out every other
-- member of staff too - see JwtAuthFilter, which now re-checks both columns against the User
-- row on every request instead of trusting the token's signature/expiry alone.
--
-- "isActive": whether this account can currently authenticate at all.
-- "tokenVersion": bumped on password change, role change, an admin password reset, or a
-- disable/enable - any already-issued token carrying an older value is rejected on its very
-- next request, regardless of how much of its validity window remains.
ALTER TABLE "User" ADD COLUMN "isActive" boolean NOT NULL DEFAULT true;
ALTER TABLE "User" ADD COLUMN "tokenVersion" integer NOT NULL DEFAULT 0;
