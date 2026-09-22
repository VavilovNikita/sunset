package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;

/**
 * Unit-level (no Spring context) coverage of the per-guest-account limiter guarding {@code POST
 * /guest/orders} and {@code POST /guest/orders/{id}/items} - same shape as {@link
 * GuestOrderRateLimiterTests}, keyed on the guest account id instead of a per-order token (see the
 * class javadoc for why).
 */
class RoomServiceOrderRateLimiterTests {

    @Test
    void twentiethAttemptFromSameAccount_isStillAllowed() {
        RoomServiceOrderRateLimiter limiter = new RoomServiceOrderRateLimiter();
        assertThatCode(() -> {
            for (int i = 0; i < 20; i++) {
                limiter.checkAllowedAndRecord("guest-account-1");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void twentyFirstAttemptFromSameAccountWithinTheWindow_isRejected() {
        RoomServiceOrderRateLimiter limiter = new RoomServiceOrderRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecord("guest-account-1");
        }

        assertThatThrownBy(() -> limiter.checkAllowedAndRecord("guest-account-1")).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void differentAccounts_haveIndependentBudgets() {
        RoomServiceOrderRateLimiter limiter = new RoomServiceOrderRateLimiter();
        for (int i = 0; i < 20; i++) {
            limiter.checkAllowedAndRecord("guest-account-1");
        }

        assertThatCode(() -> limiter.checkAllowedAndRecord("guest-account-2")).doesNotThrowAnyException();
    }
}
