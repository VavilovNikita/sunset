package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * How the poll loop behaves when Telegram is slow or gone: a call that hangs must not stop the other scheduled work,
 * and a failing poll backs off, says so once and starts over after the first success.
 */
class TelegramPollResilienceTests extends NotificationTestBase {

    @Autowired private ThreadPoolTaskScheduler scheduler;
    @Autowired private TelegramHealth health;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Logger pollLogger;

    @BeforeEach
    void captureLog() {
        pollLogger = (Logger) LoggerFactory.getLogger(TelegramPollService.class);
        logs.start();
        pollLogger.addAppender(logs);
        health.recordSuccess();
    }

    @AfterEach
    void settle() {
        pollLogger.detachAppender(logs);
        // The health object is shared with every other test of this context: leave it healthy.
        telegram.updatesFailure = null;
        health.recordSuccess();
    }

    @Test
    void aSweepThatHangs_doesNotKeepAnotherScheduledTaskFromRunning() throws Exception {
        CountDownLatch hung = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch other = new CountDownLatch(1);
        // The shape of a Telegram call that never comes back, on the application's own scheduler.
        scheduler.schedule(() -> {
            hung.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, Instant.now());
        assertThat(hung.await(5, TimeUnit.SECONDS)).isTrue();

        scheduler.schedule(other::countDown, Instant.now());

        boolean ranWhileTheFirstWasStuck = other.await(5, TimeUnit.SECONDS);
        release.countDown();
        assertThat(ranWhileTheFirstWasStuck).as("the second task ran while the first was hung").isTrue();
    }

    @Test
    void aFailingPoll_backsOffUpToAMinute_logsOncePerSeries_andResetsAfterTheFirstSuccess() {
        telegram.updatesFailure = new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer (HttpTimeoutException)");

        // 3, 6, 12, 24, 48, 60, 60 seconds: a try is made only once the pause after the previous one has run out.
        long[] pauses = {3, 6, 12, 24, 48, 60, 60};
        pollService.poll();
        assertThat(telegram.getUpdatesCalls.get()).isEqualTo(1);
        for (long pause : pauses) {
            int callsBefore = telegram.getUpdatesCalls.get();
            advance(pause - 1);
            pollService.poll();
            assertThat(telegram.getUpdatesCalls.get()).as("still waiting out %d s", pause).isEqualTo(callsBefore);
            advance(1);
            pollService.poll();
            assertThat(telegram.getUpdatesCalls.get()).as("tried again after %d s", pause).isEqualTo(callsBefore + 1);
        }

        assertThat(channelService.status().getConsecutiveFailures()).isEqualTo(8);
        assertThat(channelService.status().getFailingSince().isPresent()).isTrue();
        // One WARN for the whole series, however many tries it took.
        assertThat(logs.list.stream().filter(e -> e.getLevel() == Level.WARN)).hasSize(1);
        assertThat(logs.list.stream().filter(e -> e.getLevel() == Level.WARN).findFirst().orElseThrow().getFormattedMessage())
                .startsWith("Telegram unreachable since");

        // Telegram is back: the next try succeeds and everything starts over.
        telegram.updatesFailure = null;
        advance(60);
        pollService.poll();

        assertThat(channelService.status().getConsecutiveFailures()).isZero();
        assertThat(channelService.status().getFailingSince().isPresent()).isFalse();
        assertThat(channelService.status().getLastSuccessAt().isPresent()).isTrue();
        assertThat(logs.list.stream().filter(e -> e.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage))
                .anySatisfy(m -> assertThat(m).startsWith("Telegram is back after 8 failed attempts"));

        // A new failure starts from the base pause again and opens a second series.
        telegram.updatesFailure = new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        int calls = telegram.getUpdatesCalls.get();
        pollService.poll();
        advance(3);
        pollService.poll();
        assertThat(telegram.getUpdatesCalls.get()).isEqualTo(calls + 2);
        assertThat(logs.list.stream().filter(e -> e.getLevel() == Level.WARN)).hasSize(2);
    }

    @Test
    void whileASeriesRuns_theHousekeepingCallsAreLeftAlone() {
        telegram.updatesFailure = new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        pollService.poll(); // healthy so far, so housekeeping runs once; then the poll fails
        int commandSets = telegram.commandSets.size();

        advance(3);
        pollService.poll();

        assertThat(telegram.commandSets).hasSize(commandSets);
    }

    private void advance(long seconds) {
        clock.set(LocalDateTime.ofInstant(clock.instant(), clock.getZone()).plusSeconds(seconds).toString());
    }
}
