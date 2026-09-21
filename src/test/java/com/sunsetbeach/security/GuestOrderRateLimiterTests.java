package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;

/**
 * Unit-level (no Spring context) coverage of the per-token limiter guarding the unauthenticated
 * {@code POST /public/orders/{id}/items} - same shape as {@link BookingRateLimiterTests}, keyed
 * on the guest's {@code token} instead of an IP (see the class javadoc for why).
 */
class GuestOrderRateLimiterTests {

    @Test
    void twentiethAttemptFromSameToken_isStillAllowed() {
        GuestOrderRateLimiter limiter = new GuestOrderRateLimiter();
        assertThatCode(() -> {
            for (int i = 0; i < 20; i++) {
                limiter.checkAllowedAndRecord("token-abc");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void twentyFirstAttemptFromSameTokenWithinTheWindow_isRejected() {
        GuestOrderRateLimiter limiter = new GuestOrderRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecord("token-abc");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecord("token-abc")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void differentTokens_haveIndependentBudgets() {
        GuestOrderRateLimiter limiter = new GuestOrderRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecord("token-abc");
        }

        assertThatCode(() -> limiter.checkAllowedAndRecord("token-xyz")).doesNotThrowAnyException();
    }
}
