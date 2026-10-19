package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.service.StaffBotSettingsService;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Renews the "/" command list of linked chats after something it is built from changed ({@link BotMenuChanged}).
 *
 * <p>Like every notification it never breaks or delays the operation it accompanies: it runs only after that
 * operation's transaction committed, on its own thread (one call to Telegram can take its full timeout, and an
 * administrator's change touches every linked chat), and a failure is logged and goes no further. It writes
 * nothing to the database. <b>The keyboard under the input field is not touched here</b>: Telegram replaces it
 * only with a message that carries one, and a message sent for nothing else would be noise - the bot re-sends it
 * with the person's next reply ({@link StaffBotService}), recognising the stale one by its signature.
 */
@Component
public class BotMenuRefresher {

    private static final Logger log = LoggerFactory.getLogger(BotMenuRefresher.class);
    private static final String TELEGRAM = "TELEGRAM";

    private final TelegramClient telegram;
    private final StaffChannelLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final StaffBotSettingsService settingsService;
    private final BotMenu menu;
    private final Clock clock;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bot-menu-refresh");
        t.setDaemon(true);
        return t;
    });

    public BotMenuRefresher(
            TelegramClient telegram,
            StaffChannelLinkRepository linkRepository,
            UserRepository userRepository,
            StaffBotSettingsService settingsService,
            BotMenu menu,
            Clock clock) {
        this.telegram = telegram;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.menu = menu;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMenuChanged(BotMenuChanged event) {
        try {
            executor.execute(() -> refresh(event.userId()));
        } catch (RuntimeException e) {
            log.warn("Could not schedule a command list refresh", e);
        }
    }

    /** Body of the refresh; {@code userId} null = every linked chat. */
    void refresh(String userId) {
        try {
            if (!telegram.isConfigured()) {
                return;
            }
            List<StaffChannelLinkEntity> links = userId == null
                    ? linkRepository.findByChannel(TELEGRAM)
                    : linkRepository.findByUserIdAndChannel(userId, TELEGRAM).map(List::of).orElse(List.of());
            if (links.isEmpty()) {
                return;
            }
            StaffBotSettingsEntity settings = settingsService.getEntity();
            LocalDate today = LocalDate.now(clock);
            for (StaffChannelLinkEntity link : links) {
                try {
                    UserEntity user = userRepository.findById(link.getUserId()).filter(UserEntity::isActive).orElse(null);
                    if (user == null) {
                        continue;
                    }
                    telegram.setMyCommands(menu.commands(menu.visible(user, settings, today), menu.languageOf(user)), null, link.getExternalId());
                } catch (TelegramException e) {
                    log.warn("Could not renew a chat's command list: {}", e.getMessage());
                } catch (RuntimeException e) {
                    log.warn("Could not renew a chat's command list", e);
                }
            }
        } catch (RuntimeException e) {
            log.error("Renewing the bot's command lists failed - the change itself is unaffected", e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
