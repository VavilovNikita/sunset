package com.sunsetbeach.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The clean-up the in-memory rate limiters share. Each limiter trims a bucket only when that key
 * is used again, so a key seen once (an address, an email) used to stay in memory until the next
 * restart - and a run that rotates addresses grew the map without bound. Every limiter calls
 * {@link #evictIdle} from a {@code @Scheduled} sweep to drop the buckets with nothing left in
 * their window.
 *
 * <p>The removal is {@code computeIfPresent}, atomic against another thread's
 * {@code computeIfAbsent} on the same key; a thread that already holds the deque and records into
 * it after the removal loses that one attempt, on a key that had none left in the window - at
 * most one extra attempt, never a lockout.
 */
final class SlidingWindowBuckets {

    /** How often each limiter sweeps; shorter than every window, cheap at this hotel's traffic. */
    static final long SWEEP_INTERVAL_MS = 10 * 60 * 1000L;

    private SlidingWindowBuckets() {
    }

    static void evictIdle(ConcurrentHashMap<String, Deque<Instant>> buckets, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        for (String key : buckets.keySet()) {
            buckets.computeIfPresent(key, (k, attempts) -> {
                Instant oldest;
                while ((oldest = attempts.peekFirst()) != null && oldest.isBefore(cutoff)) {
                    attempts.pollFirst();
                }
                return attempts.isEmpty() ? null : attempts;
            });
        }
    }
}
