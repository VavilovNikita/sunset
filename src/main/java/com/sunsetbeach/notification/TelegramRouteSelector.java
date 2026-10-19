package com.sunsetbeach.notification;

import com.sunsetbeach.notification.TelegramClient.Route;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides which way {@link HttpTelegramClient} reaches Telegram. Without a proxy the answer is always
 * {@link Route#DIRECT} and nothing here ever changes. With one, {@link Route#PROXY} is the main path; after
 * {@code threshold} failed {@code getUpdates} in a row it moves to {@link Route#DIRECT}, and every
 * {@code retry} the client probes the proxy again ({@link #trialDue}) and moves back when it answers.
 *
 * <p>Only transport failures count (nothing came back: refused, timed out, reset); an answer from Telegram, even
 * an error, proves the path works. The log gets one line per switch and nothing else.
 */
final class TelegramRouteSelector {

    private static final Logger log = LoggerFactory.getLogger(TelegramRouteSelector.class);

    private final boolean proxied;
    private final int threshold;
    private final Duration retry;
    private final Clock clock;

    private Route current;
    private int failures;
    private Instant nextTrial = Instant.MIN;

    TelegramRouteSelector(boolean proxied, int threshold, Duration retry, Clock clock) {
        this.proxied = proxied;
        this.threshold = Math.max(1, threshold);
        this.retry = retry;
        this.clock = clock;
        this.current = proxied ? Route.PROXY : Route.DIRECT;
    }

    synchronized Route current() {
        return current;
    }

    /** The path {@code used} carried a getUpdates that failed to reach Telegram. */
    synchronized void failed(Route used) {
        if (!proxied || used != current) {
            return;
        }
        if (current == Route.PROXY) {
            if (++failures >= threshold) {
                current = Route.DIRECT;
                failures = 0;
                nextTrial = clock.instant().plus(retry);
                log.warn("Telegram via proxy failing, switching to direct");
            }
        } else {
            // The fallback is down too: don't sit on it for the rest of the interval, look at the proxy at once.
            nextTrial = clock.instant();
        }
    }

    /** The path {@code used} reached Telegram (any answer). */
    synchronized void answered(Route used) {
        if (used == current) {
            failures = 0;
        }
    }

    /** True while on the fallback and it is time to look at the proxy again. */
    synchronized boolean trialDue() {
        return proxied && current == Route.DIRECT && !clock.instant().isBefore(nextTrial);
    }

    synchronized void trialAnswered() {
        if (current != Route.PROXY) {
            current = Route.PROXY;
            failures = 0;
            log.info("Telegram via proxy answers again, switching back to proxy");
        }
    }

    synchronized void trialFailed() {
        nextTrial = clock.instant().plus(retry);
    }
}
