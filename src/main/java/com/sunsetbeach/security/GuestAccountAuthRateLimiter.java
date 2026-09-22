package com.sunsetbeach.security;

import com.sunsetbeach.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * Shared in-memory sliding-window limiter for {@code POST /guest-auth/register},
 * {@code /resend-verification}, and {@code /verify} - same in-memory/single-instance shape as
 * {@link LoginRateLimiter}, counting attempts regardless of success (unlike that class, which
 * only counts failures - here even a "successful" register/resend is itself the thing being
 * throttled, since without a limit a stranger could mass-trigger verification emails at an
 * arbitrary address they don't own).
 *
 * <p>Two independent buckets, both checked before either operation proceeds: per-IP (a generous
 * general flood guard) and per-email (the tighter, more targeted guard - the actual abuse this
 * exists to prevent is spamming one victim's inbox, which an attacker could otherwise route
 * around a per-IP limit alone by rotating IPs). {@code checkAllowedAndRecordForToken} is the
 * narrower per-IP-only variant for {@code /verify}, where there's no target email to key on -
 * only a token being guessed.
 */
@Component
public class GuestAccountAuthRateLimiter {

    private static final int MAX_PER_IP = 20;
    private static final Duration IP_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_PER_EMAIL = 5;
    private static final Duration EMAIL_WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowedAndRecordForEmail(String ip, String email) {
        checkAndRecord(attemptsByIp, ip, MAX_PER_IP, IP_WINDOW);
        checkAndRecord(attemptsByEmail, email.trim().toLowerCase(Locale.ROOT), MAX_PER_EMAIL, EMAIL_WINDOW);
    }

    public void checkAllowedAndRecordForToken(String ip) {
        checkAndRecord(attemptsByIp, ip, MAX_PER_IP, IP_WINDOW);
    }

    private void checkAndRecord(ConcurrentHashMap<String, Deque<Instant>> buckets, String key, int max, Duration window) {
        Deque<Instant> attempts = buckets.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        if (countRecent(attempts, window) >= max) {
            throw new TooManyRequestsException("Too many requests. Please slow down and try again.");
        }
        attempts.addLast(Instant.now());
    }

    private int countRecent(Deque<Instant> attempts, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        Instant oldest;
        while ((oldest = attempts.peekFirst()) != null && oldest.isBefore(cutoff)) {
            attempts.pollFirst();
        }
        return attempts.size();
    }
}
