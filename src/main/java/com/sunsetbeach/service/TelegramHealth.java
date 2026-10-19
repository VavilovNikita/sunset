package com.sunsetbeach.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * What the poll loop knows about whether Telegram answers: when it last did, whether a series of failures is
 * running and, while it is, how long to wait before the next try. In memory on purpose - it describes this
 * process's own connection and is worth nothing after a restart. Only {@code getUpdates} feeds it: that call is
 * made all the time, so it is the heartbeat; a single failed send says little.
 *
 * <p>The pause after a failure doubles ({@code base} -> 2x -> 4x ... up to {@code max}) and starts over after the
 * first success. {@link #recordFailure} tells the caller whether this failure opened a series, and
 * {@link #recordSuccess} whether it closed one, so the log gets one line for each and nothing in between.
 */
@Component
public class TelegramHealth {

    /** A series that has just ended. */
    public record Recovery(int failures, Duration duration) {
    }

    /** A copy of the state for the status endpoint. */
    public record Snapshot(Instant lastSuccessAt, Instant failingSince, int consecutiveFailures) {
    }

    private final Clock clock;
    private final Duration base;
    private final Duration max;

    private Instant lastSuccessAt;
    private Instant failingSince;
    private int failures;
    private Instant retryNotBefore = Instant.MIN;

    public TelegramHealth(
            Clock clock,
            @Value("${app.notifications.telegram.backoff-base-seconds:3}") long baseSeconds,
            @Value("${app.notifications.telegram.backoff-max-seconds:60}") long maxSeconds) {
        this.clock = clock;
        this.base = Duration.ofSeconds(Math.max(1, baseSeconds));
        this.max = Duration.ofSeconds(Math.max(baseSeconds, maxSeconds));
    }

    /** False while the pause after the last failure has not run out. */
    public synchronized boolean mayAttempt() {
        return !clock.instant().isBefore(retryNotBefore);
    }

    /** True while a series of failures is running. */
    public synchronized boolean failing() {
        return failures > 0;
    }

    /** @return true if this failure opened a new series (the caller logs it once) */
    public synchronized boolean recordFailure() {
        Instant now = clock.instant();
        boolean opened = failures == 0;
        if (opened) {
            failingSince = now;
        }
        failures++;
        long shift = Math.min(failures - 1, 16);
        Duration pause = base.multipliedBy(1L << shift);
        retryNotBefore = now.plus(pause.compareTo(max) > 0 ? max : pause);
        return opened;
    }

    /** @return the series this success ended, if there was one */
    public synchronized Optional<Recovery> recordSuccess() {
        Instant now = clock.instant();
        Optional<Recovery> recovery = failures == 0 ? Optional.empty() : Optional.of(new Recovery(failures, Duration.between(failingSince, now)));
        lastSuccessAt = now;
        failingSince = null;
        failures = 0;
        retryNotBefore = Instant.MIN;
        return recovery;
    }

    public synchronized Instant failingSince() {
        return failingSince;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(lastSuccessAt, failingSince, failures);
    }
}
