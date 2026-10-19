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
 */
@Service
public class TelegramPollService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollService.class);

    private final TelegramClient telegram;
    private final TelegramBotStateRepository stateRepository;
    private final NotificationChannelService channelService;
    private final StaffBotService staffBot;
    private final TransactionTemplate tx;
    private final int longPollSeconds;

    public TelegramPollService(
            TelegramClient telegram,
            TelegramBotStateRepository stateRepository,
            NotificationChannelService channelService,
            StaffBotService staffBot,
            PlatformTransactionManager transactionManager,
            @Value("${app.notifications.telegram.long-poll-seconds:1}") int longPollSeconds) {
        this.telegram = telegram;
        this.stateRepository = stateRepository;
        this.channelService = channelService;
        this.staffBot = staffBot;
        this.tx = new TransactionTemplate(transactionManager);
        this.longPollSeconds = longPollSeconds;
    }

    @Scheduled(fixedDelayString = "${app.notifications.telegram.poll-interval-ms:3000}", initialDelayString = "${app.notifications.initial-delay-ms:10000}")
    public void poll() {
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
        // botUsername is a JsonNullable (nullable in the spec): never null itself, "unknown" is not present.
        if (!channelService.status().getBotUsername().isPresent()) {
            channelService.refreshBotUsername();
        }
        staffBot.ensureDefaultCommands();
        long offset = stateRepository.findById(1).map(TelegramBotStateEntity::getUpdateOffset).orElse(0L);
        List<TelegramUpdate> updates;
        try {
            updates = telegram.getUpdates(offset, longPollSeconds);
        } catch (TelegramException e) {
            log.warn("Telegram getUpdates failed: {}", e.getMessage());
            return 0;
        }
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
