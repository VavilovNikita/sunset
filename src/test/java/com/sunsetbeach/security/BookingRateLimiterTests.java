package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;

/**
 * Unit-level (no Spring context) coverage of the per-IP limiter guarding the unauthenticated
 * {@code POST /bookings} - see the class javadoc for what this limiter does and doesn't defend
 * against (single-instance, in-memory, and only as trustworthy as the reverse proxy in front of
 * it for the IP it keys on).
 */
class BookingRateLimiterTests {

    @Test
    void eighthAttemptFromSameIp_isStillAllowed() {
        BookingRateLimiter limiter = new BookingRateLimiter();
        assertThatCode(() -> {
            for (int i = 0; i < 8; i++) {
                limiter.checkAllowedAndRecord("203.0.113.10");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void ninthAttemptFromSameIpWithinTheWindow_isRejected() {
        BookingRateLimiter limiter = new BookingRateLimiter();
        for (int i = 0; i < 8; i++) {
            limiter.checkAllowedAndRecord("203.0.113.10");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecord("203.0.113.10")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void differentIps_haveIndependentBudgets() {
        BookingRateLimiter limiter = new BookingRateLimiter();
        for (int i = 0; i < 8; i++) {
            limiter.checkAllowedAndRecord("203.0.113.10");
        }

        assertThatCode(() -> limiter.checkAllowedAndRecord("203.0.113.20")).doesNotThrowAnyException();
    }
}
