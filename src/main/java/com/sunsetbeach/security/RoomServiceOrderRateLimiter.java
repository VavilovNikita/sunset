package com.sunsetbeach.security;

import com.sunsetbeach.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * Per-guest-account sliding-window limiter for the two {@code GuestOrder} write endpoints
 * ({@code POST /guest/orders}, {@code POST /guest/orders/{id}/items}) - same shape as {@link
 * GuestOrderRateLimiter} (dine-in QR ordering), keyed on the guest account id instead of a
 * per-order token: room-service ordering is authenticated (a guest JWT), so there's a real
 * account identity to key on here, unlike the unauthenticated, token-only dine-in door.
 */
@Component
public class RoomServiceOrderRateLimiter {

    private static final int MAX_ATTEMPTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByGuestAccountId = new ConcurrentHashMap<>();

    public void checkAllowedAndRecord(String guestAccountId) {
        Deque<Instant> attempts = attemptsByGuestAccountId.computeIfAbsent(guestAccountId, k -> new ConcurrentLinkedDeque<>());
        if (countRecent(attempts) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many requests. Please slow down and try again.");
        }
        attempts.addLast(Instant.now());
    }

    private int countRecent(Deque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(WINDOW);
        Instant oldest;
        while ((oldest = attempts.peekFirst()) != null && oldest.isBefore(cutoff)) {
            attempts.pollFirst();
        }
        return attempts.size();
    }
}
