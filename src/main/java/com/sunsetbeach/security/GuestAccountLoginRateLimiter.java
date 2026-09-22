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
 * The guest-account analogue of {@link LoginRateLimiter} for {@code POST /guest-auth/login} -
 * same shape, same limits, its own bucket (a flood of guest login attempts must not lock out
 * staff logins sharing the same IP, and vice versa). See that class's own javadoc for the
 * in-memory/single-instance caveat.
 */
@Component
public class GuestAccountLoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Deque<Instant>> failuresByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String ip, String email) {
        Deque<Instant> failures = failuresByKey.get(key(ip, email));
        if (failures != null && countRecent(failures) >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
        }
    }

    public void recordFailure(String ip, String email) {
        Deque<Instant> failures = failuresByKey.computeIfAbsent(key(ip, email), k -> new ConcurrentLinkedDeque<>());
        failures.addLast(Instant.now());
        countRecent(failures);
    }

    public void recordSuccess(String ip, String email) {
        failuresByKey.remove(key(ip, email));
    }

    private int countRecent(Deque<Instant> failures) {
        Instant cutoff = Instant.now().minus(WINDOW);
        Instant oldest;
        while ((oldest = failures.peekFirst()) != null && oldest.isBefore(cutoff)) {
            failures.pollFirst();
        }
        return failures.size();
    }

    private static String key(String ip, String email) {
        return ip + "|" + email.trim().toLowerCase(Locale.ROOT);
    }
}
