package com.sunsetbeach.service;

import com.sunsetbeach.bot.StaffBotService;
import com.sunsetbeach.entity.TelegramBotStateEntity;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.repository.TelegramBotStateRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads what people write to the bot and the buttons they press (linking, commands, shift requests - all routed by
 * {@link StaffBotService}) by asking Telegram
 * ({@code getUpdates}) on a fixed delay - the project's usual {@code @Scheduled} sweep. No webhook:
 * nothing public listens, no {@code SecurityConfig} rule is needed, and it works from a developer's
 * machine. Telegram only ever answers calls this server makes.
 *
 * <p>The offset (the id after the last update handled) is kept in the database and moved after each
 * update, so a restart neither replays a message nor loses one; an update that fails to be handled is
 * logged and skipped rather than retried forever. With no {@code TELEGRAM_BOT_TOKEN} the sweep does
 * nothing at all. The bot's own name ({@code getMe}) is fetched here until it is known and cached.
 *
 * <p><b>A real long poll on a thread of its own.</b> {@code getUpdates} waits up to {@code long-poll-seconds}
 * (25) for something to arrive, so a press reaches us at once and Telegram is asked a few times a minute, not
 * every few seconds; the short {@code poll-interval-ms} only separates one request from the next. It does not
 * share a thread with the other sweeps ({@code spring.task.scheduling.pool.size}): a call that hangs here must
 * not hold up the gym, the printers or the notification sender, nor they it.
 *
 * <p><b>A failure backs off and is logged once.</b> After a failed {@code getUpdates} the next try waits 3, 6,
 * 12 ... up to 60 seconds ({@link TelegramHealth}) and starts over after a success. The log gets one WARN when a
 * series of failures begins and one INFO when it ends; {@code GET /notifications/status} shows the same state.
 * While a series runs, the housekeeping calls (bot name, command list) are left for after it - they would only
 * add more timeouts.
 */
@Service
public class TelegramPollService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollService.class);

    private final TelegramClient telegram;
    private final TelegramBotStateRepository stateRepository;
    private final NotificationChannelService channelService;
    private final StaffBotService staffBot;
    private final TransactionTemplate tx;
    private final TelegramHealth health;
    private final int longPollSeconds;

    public TelegramPollService(
            TelegramClient telegram,
            TelegramBotStateRepository stateRepository,
            NotificationChannelService channelService,
            StaffBotService staffBot,
            PlatformTransactionManager transactionManager,
            TelegramHealth health,
            @Value("${app.notifications.telegram.long-poll-seconds:25}") int longPollSeconds) {
        this.telegram = telegram;
        this.stateRepository = stateRepository;
        this.channelService = channelService;
        this.staffBot = staffBot;
        this.tx = new TransactionTemplate(transactionManager);
        this.health = health;
        this.longPollSeconds = longPollSeconds;
    }

    @Scheduled(fixedDelayString = "${app.notifications.telegram.poll-interval-ms:200}", initialDelayString = "${app.notifications.initial-delay-ms:10000}")
    public void poll() {
        if (!health.mayAttempt()) {
            return;
        }
        try {
            pollOnce();
        } catch (RuntimeException e) {
            log.error("Telegram poll failed", e);
        }
    }

    /** One getUpdates round; returns how many updates were handled. */
    public int pollOnce() {
        if (!telegram.isConfigured()) {
            return 0;
        }
        if (!health.failing()) {
            // botUsername is a JsonNullable (nullable in the spec): never null itself, "unknown" is not present.
            if (!channelService.status().getBotUsername().isPresent()) {
                channelService.refreshBotUsername();
            }
            staffBot.ensureDefaultCommands();
        }
        long offset = stateRepository.findById(1).map(TelegramBotStateEntity::getUpdateOffset).orElse(0L);
        List<TelegramUpdate> updates;
        try {
            updates = telegram.getUpdates(offset, longPollSeconds);
        } catch (TelegramException e) {
            if (health.recordFailure()) {
                log.warn("Telegram unreachable since {}: {}", health.failingSince(), e.getMessage());
            }
            return 0;
        }
        health.recordSuccess().ifPresent(r -> log.info("Telegram is back after {} failed attempts, {} min", r.failures(), r.duration().toMinutes()));
        int handled = 0;
        for (TelegramUpdate update : updates) {
            try {
                staffBot.handle(update);
                handled++;
            } catch (RuntimeException e) {
                log.error("Telegram update {} could not be handled", update.updateId(), e);
            }
            // Moved even after a failure: one bad update must not block the queue behind it.
            long next = update.updateId() + 1;
            tx.executeWithoutResult(s -> {
                TelegramBotStateEntity state = stateRepository.findById(1).orElseThrow();
                if (next > state.getUpdateOffset()) {
                    state.setUpdateOffset(next);
                    stateRepository.save(state);
                }
            });
        }
        return handled;
    }
}
