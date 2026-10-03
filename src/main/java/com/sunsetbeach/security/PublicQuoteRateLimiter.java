package com.sunsetbeach.security;

import com.sunsetbeach.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * Per-address sliding-window limiter for the unauthenticated {@code GET /public/rooms/{id}/quote}
 * - the same shape as {@link BookingRateLimiter}, but its own bucket and a much larger budget: a
 * guest changes dates several times before booking once, and sharing {@code BookingRateLimiter}'s
 * eight-an-hour bucket would let quoting use up the booking attempts themselves. It exists to stop
 * a script hammering the pricing/availability engine, not to meter real guests. Same in-memory,
 * single-instance limits as that class, and the same {@link ClientIpResolver} caveat.
 */
@Component
public class PublicQuoteRateLimiter {

    private static final int MAX_ATTEMPTS = 120;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public void checkAllowedAndRecord(String ip) {
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        if (countRecent(attempts) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many price requests from this address. Please try again later.");
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
