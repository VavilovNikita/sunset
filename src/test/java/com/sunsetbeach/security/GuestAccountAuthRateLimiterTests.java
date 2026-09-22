package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;

/**
 * Unit-level (no Spring context) coverage of the shared limiter guarding
 * {@code /guest-auth/register}, {@code /resend-verification}, and {@code /verify} - same shape as
 * {@link BookingRateLimiterTests}/{@link GuestOrderRateLimiterTests}, a fresh instance per test so
 * the per-IP/per-email windows never leak between assertions.
 */
class GuestAccountAuthRateLimiterTests {

    @Test
    void twentiethAttemptFromSameIp_isStillAllowed() {
        GuestAccountAuthRateLimiter limiter = new GuestAccountAuthRateLimiter();
        assertThatCode(() -> {
            for (int i = 0; i < 20; i++) {
                limiter.checkAllowedAndRecordForEmail("203.0.113.10", "guest-" + i + "@example.com");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void twentyFirstAttemptFromSameIpWithinTheWindow_isRejected() {
        GuestAccountAuthRateLimiter limiter = new GuestAccountAuthRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecordForEmail("203.0.113.10", "guest-" + i + "@example.com");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecordForEmail("203.0.113.10", "overflow@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void sixthAttemptForSameEmailFromDifferentIps_isRejected() {
        GuestAccountAuthRateLimiter limiter = new GuestAccountAuthRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowedAndRecordForEmail("203.0.113." + i, "victim@example.com");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecordForEmail("203.0.113.99", "victim@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void differentIpsAndEmails_haveIndependentBudgets() {
        GuestAccountAuthRateLimiter limiter = new GuestAccountAuthRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecordForEmail("203.0.113.10", "guest-" + i + "@example.com");
        }

        assertThatCode(() -> limiter.checkAllowedAndRecordForEmail("203.0.113.20", "someone-else@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAllowedAndRecordForToken_sharesThePerIpBudgetWithTheEmailVariant() {
        GuestAccountAuthRateLimiter limiter = new GuestAccountAuthRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecordForToken("203.0.113.10");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecordForEmail("203.0.113.10", "guest@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
