package com.sunsetbeach.security;

/**
 * Shared by every login check that looks an account up by email before comparing a password
 * (currently {@code AuthController#login} and {@code GuestAccountService#login}). Without this,
 * {@code entity == null || !passwordEncoder.matches(...)} short-circuits on a nonexistent email
 * and skips the BCrypt comparison entirely - a real, measurable timing difference (BCrypt cost 12
 * costs tens of milliseconds) between "no such account" and "wrong password for a real account",
 * on endpoints whose whole design intent is that a caller can never distinguish those two cases.
 * Comparing against this fixed hash instead keeps the cost constant regardless of whether an
 * account was found.
 */
public final class PasswordTimingNormalization {

    /**
     * Not a real credential - a BCrypt (cost 12) hash of a fixed, arbitrary string, generated once
     * and hardcoded purely so a missing account pays the same comparison cost as a real one.
     */
    public static final String DUMMY_HASH = "$2a$12$0kF0BPr7GTwwL1tk5SWXWO9kz7T2h6tZ.e2ZgJPgR3SVSd1Ouyk1e";

    private PasswordTimingNormalization() {
    }
}
