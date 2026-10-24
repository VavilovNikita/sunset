package com.sunsetbeach.security;

import com.sunsetbeach.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Per-IP sliding-window limiter for the unauthenticated {@code POST /bookings}: at most
 * MAX_ATTEMPTS booking-creation attempts per IP within WINDOW, counted regardless of whether the
 * attempt actually succeeds (unlike {@link com.sunsetbeach.security.LoginRateLimiter}, which
 * only counts *failed* logins - here every attempt occupies inventory or at least DB work, so
 * every attempt counts). Same limitations as LoginRateLimiter: in-memory and single-instance
 * (resets on restart, and a multi-instance deployment would need a shared store instead), and
 * the IP itself comes from {@link ClientIpResolver} (the last {@code X-Forwarded-For} hop, written
 * by the proxy in front of this app - see that class for what it can and can't tell apart).
 *
 * <p>This alone does not close the "flood the inventory with fake bookings" business risk - it
 * only slows down a single, unproxied source. See {@link com.sunsetbeach.service.BookingExpiryService}
 * for the second, more important control: bookings this limiter fails to prevent still expire on
 * their own instead of holding inventory indefinitely.
 */
@Component
public class BookingRateLimiter {

    private static final int MAX_ATTEMPTS = 8;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    public void checkAllowedAndRecord(String ip) {
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        if (countRecent(attempts) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many booking requests from this address. Please try again later.");
        }
        attempts.addLast(Instant.now());
    }

    /** Drops buckets with nothing left in the window (see {@link SlidingWindowBuckets}). */
    @Scheduled(fixedDelay = SlidingWindowBuckets.SWEEP_INTERVAL_MS)
    void evictIdle() {
        SlidingWindowBuckets.evictIdle(attemptsByIp, WINDOW);
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
