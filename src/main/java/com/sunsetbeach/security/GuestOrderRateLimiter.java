package com.sunsetbeach.security;

import com.sunsetbeach.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * Per-token sliding-window limiter for the unauthenticated {@code POST /public/orders/{id}/items}
 * - at most MAX_ATTEMPTS add-item attempts per {@code guestAccessToken} within WINDOW, counted
 * regardless of success, same shape as {@link BookingRateLimiter} (see that class's own javadoc
 * for the in-memory/single-instance caveat, which applies here too). Keyed on the token rather
 * than the caller's IP: several phones legitimately share one table's token, and a proxied
 * deployment (see {@link ClientIpResolver}) would otherwise bucket them all under one address
 * anyway - the token is the actual unit this endpoint's blast radius (one table's kitchen ticket
 * spam) is scoped to.
 */
@Component
public class GuestOrderRateLimiter {

    private static final int MAX_ATTEMPTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByToken = new ConcurrentHashMap<>();

    public void checkAllowedAndRecord(String token) {
        Deque<Instant> attempts = attemptsByToken.computeIfAbsent(token, k -> new ConcurrentLinkedDeque<>());
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
