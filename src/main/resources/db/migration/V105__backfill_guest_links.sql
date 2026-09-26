-- Backfills the Guest links that GuestLinkService now makes going forward, for rows that existed
-- before it did. Touches production data - take a dump before deploying.
--
-- Matching is by case-insensitive, trimmed email, and only ever to a Guest card that is the
-- *sole* card with that email - an email shared by several Guest rows is skipped, never guessed
-- at, same rule as the live find-or-create path. No Guest card is created here: a no-match row
-- stays unlinked (the live path creates cards only for new bookings/verifications).
--
-- Booking: only rows whose guestId is still NULL are touched, so a manual staff link
-- (PUT /bookings/{id}/guest) is never overwritten.
--
-- Counts are logged via RAISE NOTICE, which Flyway prints as "DB: ..." in the application log.
DO $$
DECLARE
    account_linked    integer;
    account_ambiguous integer;
    account_no_match  integer;
    booking_linked    integer;
    booking_ambiguous integer;
    booking_no_match  integer;
    booking_no_email  integer;
    booking_manual    integer;
BEGIN
    CREATE TEMP TABLE guest_email_match ON COMMIT DROP AS
        SELECT lower(trim(email)) AS norm_email, min(id) AS guest_id, count(*) AS n
        FROM "Guest"
        WHERE email IS NOT NULL AND trim(email) <> ''
        GROUP BY lower(trim(email));

    -- GuestAccount -> Guest. GuestAccount.email is already stored lowercased (V89).
    SELECT count(*) INTO account_ambiguous
    FROM "GuestAccount" a JOIN guest_email_match m ON m.norm_email = lower(trim(a.email))
    WHERE a."guestId" IS NULL AND m.n > 1;

    SELECT count(*) INTO account_no_match
    FROM "GuestAccount" a
    WHERE a."guestId" IS NULL
      AND NOT EXISTS (SELECT 1 FROM guest_email_match m WHERE m.norm_email = lower(trim(a.email)));

    UPDATE "GuestAccount" a
    SET "guestId" = m.guest_id
    FROM guest_email_match m
    WHERE a."guestId" IS NULL AND m.n = 1 AND m.norm_email = lower(trim(a.email));
    GET DIAGNOSTICS account_linked = ROW_COUNT;

    -- Booking -> Guest.
    SELECT count(*) INTO booking_manual FROM "Booking" WHERE "guestId" IS NOT NULL;

    SELECT count(*) INTO booking_no_email
    FROM "Booking" b
    WHERE b."guestId" IS NULL AND (b."guestEmail" IS NULL OR trim(b."guestEmail") = '');

    SELECT count(*) INTO booking_ambiguous
    FROM "Booking" b JOIN guest_email_match m ON m.norm_email = lower(trim(b."guestEmail"))
    WHERE b."guestId" IS NULL AND m.n > 1;

    SELECT count(*) INTO booking_no_match
    FROM "Booking" b
    WHERE b."guestId" IS NULL
      AND b."guestEmail" IS NOT NULL AND trim(b."guestEmail") <> ''
      AND NOT EXISTS (SELECT 1 FROM guest_email_match m WHERE m.norm_email = lower(trim(b."guestEmail")));

    UPDATE "Booking" b
    SET "guestId" = m.guest_id
    FROM guest_email_match m
    WHERE b."guestId" IS NULL AND m.n = 1 AND m.norm_email = lower(trim(b."guestEmail"));
    GET DIAGNOSTICS booking_linked = ROW_COUNT;

    RAISE NOTICE 'V105 GuestAccount->Guest: linked=%, skipped ambiguous=%, skipped no match=%',
        account_linked, account_ambiguous, account_no_match;
    RAISE NOTICE 'V105 Booking->Guest: linked=%, skipped ambiguous=%, skipped no match=%, skipped no email=%, already linked (untouched)=%',
        booking_linked, booking_ambiguous, booking_no_match, booking_no_email, booking_manual;
END $$;
