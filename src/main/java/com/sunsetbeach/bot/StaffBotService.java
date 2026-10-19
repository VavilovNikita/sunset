package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.notification.TelegramClient.Button;
import com.sunsetbeach.notification.TelegramClient.Command;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.repository.StaffActionRepository;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.service.BotActionIds;
import com.sunsetbeach.service.NotificationChannelService;
import com.sunsetbeach.service.NotificationSettingsService;
import com.sunsetbeach.service.StaffBotSettingsService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The staff bot: routes what linked employees type and tap to the features that registered for it.
 *
 * <p><b>Linking stays where it was</b> ({@code /start <code>}, {@code /stop}, anything from a chat nobody is
 * linked to): {@link NotificationChannelService#handleUpdate}. Everything else is for a linked, active
 * employee and goes to {@link BotCommandHandler} (a command or its menu button), {@link BotActionHandler}
 * (a pressed button) or {@link BotDialogueHandler} (the next free-text message of an unfinished dialogue).
 * A new screen is a new bean of one of those types; this class does not change.
 *
 * <p><b>A button carries only an opaque id</b> (callback data {@code a:<id>}, 14 bytes). The row it names
 * ({@code StaffAction}) holds what it does, to whom it belongs and when it stops being valid; a press is
 * honoured only from the chat of that person, who must still be an active employee - and the handler
 * re-checks what they may do <em>now</em>. Pressing from another chat, or after the person lost the right,
 * changes nothing.
 *
 * <p>Any typed command discards an unfinished dialogue. Nothing here throws: a failing update is logged and
 * the poll moves on.
 */
@Service
public class StaffBotService {

    private static final Logger log = LoggerFactory.getLogger(StaffBotService.class);
    private static final String TELEGRAM = "TELEGRAM";
    private static final Duration BUTTON_LIFETIME = Duration.ofHours(24);
    private static final String CALLBACK_PREFIX = "a:";

    private final TelegramClient telegram;
    private final StaffChannelLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final StaffActionRepository actionRepository;
    private final BotConversations conversations;
    private final StaffBotSettingsService settingsService;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationChannelService channelService;
    private final List<BotCommandHandler> commandHandlers;
    private final Map<String, BotActionHandler> actionHandlers = new HashMap<>();
    private final Map<String, BotDialogueHandler> dialogueHandlers = new HashMap<>();
    private final Clock clock;
    private final TransactionTemplate tx;
    private final AtomicBoolean defaultCommandsSet = new AtomicBoolean(false);

    public StaffBotService(
            TelegramClient telegram,
            StaffChannelLinkRepository linkRepository,
            UserRepository userRepository,
            StaffActionRepository actionRepository,
            BotConversations conversations,
            StaffBotSettingsService settingsService,
            NotificationSettingsService notificationSettingsService,
            NotificationChannelService channelService,
            List<BotCommandHandler> commandHandlers,
            List<BotActionHandler> actionHandlers,
            List<BotDialogueHandler> dialogueHandlers,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.telegram = telegram;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.conversations = conversations;
        this.settingsService = settingsService;
        this.notificationSettingsService = notificationSettingsService;
        this.channelService = channelService;
        this.commandHandlers = commandHandlers.stream().sorted(Comparator.comparingInt(BotCommandHandler::order)).toList();
        actionHandlers.forEach(h -> h.kinds().forEach(kind -> this.actionHandlers.put(kind, h)));
        dialogueHandlers.forEach(h -> this.dialogueHandlers.put(h.state(), h));
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ---- entry point -----------------------------------------------------------------------------------

    /** Handles one update and answers it. Never throws. */
    public void handle(TelegramUpdate update) {
        if (update.chatId() == null) {
            return;
        }
        try {
            if (update.isCallback()) {
                handleCallback(update);
                return;
            }
            if (update.text() == null) {
                return;
            }
            String text = update.text().strip();
            String command = commandOf(text);
            Optional<UserEntity> user = linkedUser(update.chatId());
            boolean linking = "/stop".equals(command) || ("/start".equals(command) && text.contains(" "));
            if (user.isEmpty() || linking) {
                channelService.handleUpdate(update);
                return;
            }
            handleMessage(context(update.chatId(), null, null, user.get(), null), text, command);
        } catch (RuntimeException e) {
            log.error("Handling a bot update failed", e);
        }
    }

    /** Sets the "/" command list everybody sees (once per run, retried while Telegram can't be reached). */
    public void ensureDefaultCommands() {
        if (!telegram.isConfigured() || defaultCommandsSet.get()) {
            return;
        }
        try {
            for (NotificationLanguage language : NotificationLanguage.values()) {
                List<Command> commands = new ArrayList<>();
                commands.add(new Command("menu", NotificationMessages.render(Key.BOT_MENU, language, Map.of())));
                for (BotCommandHandler h : commandHandlers) {
                    if (h.forEveryone()) {
                        commands.add(new Command(h.command(), NotificationMessages.render(h.label(), language, Map.of())));
                    }
                }
                // Telegram's own language codes; the default (no code) is English.
                telegram.setMyCommands(commands, language.getValue().toLowerCase(Locale.ROOT), null);
                if (language == NotificationLanguage.EN) {
                    telegram.setMyCommands(commands, null, null);
                }
            }
            defaultCommandsSet.set(true);
        } catch (TelegramException e) {
            log.warn("Could not set the bot's command list: {}", e.getMessage());
        }
    }

    // ---- messages --------------------------------------------------------------------------------------

    private void handleMessage(BotContext ctx, String text, String command) {
        if (text.startsWith("/")) {
            conversations.clear(ctx.chatId());
            String arguments = text.contains(" ") ? text.substring(text.indexOf(' ')).strip() : "";
            switch (command) {
                case "/start", "/menu", "/help" -> sendMenu(ctx);
                case "/cancel" -> {
                    deliver(ctx, List.of(BotReply.text(ctx.t(Key.BOT_CANCELLED))));
                    sendMenu(ctx);
                }
                default -> {
                    BotCommandHandler handler = commandHandlers.stream().filter(h -> ("/" + h.command()).equals(command)).findFirst().orElse(null);
                    if (handler == null) {
                        sendMenu(ctx);
                    } else {
                        runCommand(ctx, handler, arguments);
                    }
                }
            }
            return;
        }
        BotCommandHandler byLabel = commandHandlers.stream().filter(h -> labelMatches(h, text)).findFirst().orElse(null);
        if (byLabel != null) {
            conversations.clear(ctx.chatId());
            runCommand(ctx, byLabel, "");
            return;
        }
        Optional<BotConversationEntity> conversation = conversations.current(ctx.chatId());
        if (conversation.isPresent() && conversation.get().getUserId().equals(ctx.user().getId())) {
            BotDialogueHandler dialogue = dialogueHandlers.get(conversation.get().getState());
            if (dialogue != null) {
                deliver(ctx, dialogue.onText(ctx, conversation.get(), text));
                return;
            }
        }
        sendMenu(ctx);
    }

    private static boolean labelMatches(BotCommandHandler handler, String text) {
        for (NotificationLanguage language : NotificationLanguage.values()) {
            if (NotificationMessages.render(handler.label(), language, Map.of()).equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    private void runCommand(BotContext ctx, BotCommandHandler handler, String arguments) {
        if (!handler.enabled(ctx.settings())) {
            deliver(ctx, List.of(BotReply.text(ctx.t(Key.BOT_FEATURE_OFF))));
            return;
        }
        if (!handler.availableTo(ctx.user(), ctx.today())) {
            deliver(ctx, List.of(BotReply.text(ctx.t(Key.BOT_NOT_ALLOWED))));
            return;
        }
        deliver(ctx, handler.handle(ctx, arguments));
    }

    private void sendMenu(BotContext ctx) {
        List<BotCommandHandler> visible = commandHandlers.stream()
                .filter(h -> h.enabled(ctx.settings()) && h.availableTo(ctx.user(), ctx.today()))
                .toList();
        List<List<String>> keyboard = new ArrayList<>();
        List<String> row = new ArrayList<>();
        for (BotCommandHandler h : visible) {
            row.add(NotificationMessages.render(h.label(), ctx.language(), Map.of()));
            if (row.size() == 2) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            keyboard.add(row);
        }
        try {
            telegram.sendMessage(ctx.chatId(), ctx.t(Key.BOT_MENU), null, keyboard);
            // This person's own "/" list: the shared one plus what only they may use.
            List<Command> commands = new ArrayList<>();
            commands.add(new Command("menu", ctx.t(Key.BOT_MENU)));
            visible.forEach(h -> commands.add(new Command(h.command(), NotificationMessages.render(h.label(), ctx.language(), Map.of()))));
            telegram.setMyCommands(commands, null, ctx.chatId());
        } catch (TelegramException e) {
            log.warn("Could not send the menu: {}", e.getMessage());
        }
    }

    // ---- buttons ---------------------------------------------------------------------------------------

    /**
     * <b>The press is acknowledged first, before anything is looked up or written.</b> Telegram keeps the
     * spinner on a button until it is answered and refuses an answer to a press older than about 15 seconds
     * (HTTP 400), so answering after the work meant a slow database or a slow Telegram turned into a dead
     * button. The answer is empty and has a short timeout; if it fails the press is still handled. What the
     * person should know - the outcome, or why nothing happened - follows as a message or an edit of the
     * message the button sat under.
     */
    private void handleCallback(TelegramUpdate update) {
        acknowledge(update);
        String data = update.callbackData();
        StaffActionEntity action = data != null && data.startsWith(CALLBACK_PREFIX)
                ? actionRepository.findById(data.substring(CALLBACK_PREFIX.length())).orElse(null)
                : null;
        Optional<UserEntity> user = linkedUser(update.chatId());
        if (user.isEmpty()) {
            try {
                telegram.sendMessage(update.chatId(), NotificationMessages.render(Key.BOT_NOT_ALLOWED, defaultLanguage(), Map.of()));
            } catch (TelegramException e) {
                log.warn("Could not answer a Telegram chat: {}", e.getMessage());
            }
            return;
        }
        BotContext ctx = context(update.chatId(), update.callbackQueryId(), update.messageId(), user.get(), action);
        // A button is for one person: another chat pressing it (a forwarded message) does nothing at all.
        if (action == null || !action.getUserId().equals(user.get().getId())
                || (action.getExpiresAt() != null && action.getExpiresAt().isBefore(TimestampFormat.nowUtc(clock)))) {
            say(ctx, ctx.t(action != null && !action.getUserId().equals(user.get().getId()) ? Key.BOT_NOT_ALLOWED : Key.BOT_BUTTON_EXPIRED));
            return;
        }
        BotActionHandler handler = actionHandlers.get(action.getKind());
        if (handler == null) {
            say(ctx, ctx.t(Key.BOT_BUTTON_EXPIRED));
            return;
        }
        List<BotReply> replies;
        try {
            replies = handler.handle(ctx, action);
        } catch (ForbiddenException e) {
            say(ctx, ctx.t(Key.BOT_NOT_ALLOWED));
            return;
        }
        // A toast needs the press to still be unanswered, which it no longer is: a reply that was only a toast
        // becomes a message, and one that has its own text or edit already says it.
        deliver(ctx, replies.stream().map(r -> r.text() == null && r.toast() != null && !r.toast().isBlank() ? BotReply.text(r.toast()) : r).toList());
    }

    private void acknowledge(TelegramUpdate update) {
        try {
            telegram.answerCallbackQuery(update.callbackQueryId(), null);
        } catch (TelegramException e) {
            log.warn("Could not answer a button press: {}", e.getMessage());
        }
    }

    private void say(BotContext ctx, String text) {
        deliver(ctx, List.of(BotReply.text(text)));
    }

    // ---- sending -----------------------------------------------------------------------------------------

    private void deliver(BotContext ctx, List<BotReply> replies) {
        for (BotReply reply : replies) {
            if (reply.text() == null) {
                continue;
            }
            List<List<Button>> buttons = reply.rows().isEmpty() ? null : toTelegram(ctx, reply);
            try {
                if (reply.isEditCurrent() && ctx.messageId() != null) {
                    telegram.editMessageText(ctx.chatId(), ctx.messageId(), reply.text(), buttons);
                } else {
                    telegram.sendMessage(ctx.chatId(), reply.text(), buttons, null);
                }
            } catch (TelegramException e) {
                log.warn("Could not answer a Telegram chat: {}", e.getMessage());
            }
        }
    }

    /** Each button becomes a StaffAction row for this person; only its id goes to Telegram. */
    private List<List<Button>> toTelegram(BotContext ctx, BotReply reply) {
        LocalDateTime expires = TimestampFormat.nowUtc(clock).plus(BUTTON_LIFETIME);
        List<List<Button>> out = new ArrayList<>();
        tx.executeWithoutResult(status -> {
            for (List<BotReply.Button> row : reply.rows()) {
                List<Button> converted = new ArrayList<>();
                for (BotReply.Button b : row) {
                    StaffActionEntity a = new StaffActionEntity();
                    a.setId(BotActionIds.next());
                    a.setUserId(ctx.user().getId());
                    a.setKind(b.kind());
                    a.setSubjectId(b.subjectId());
                    a.setArg(b.arg());
                    a.setExpiresAt(expires);
                    actionRepository.save(a);
                    converted.add(new Button(b.label(), CALLBACK_PREFIX + a.getId()));
                }
                out.add(converted);
            }
        });
        return out;
    }

    // ---- who ---------------------------------------------------------------------------------------------

    private Optional<UserEntity> linkedUser(String chatId) {
        return linkRepository.findByChannelAndExternalId(TELEGRAM, chatId)
                .map(StaffChannelLinkEntity::getUserId)
                .flatMap(userRepository::findById)
                .filter(UserEntity::isActive);
    }

    private BotContext context(String chatId, String callbackQueryId, String messageId, UserEntity user, StaffActionEntity action) {
        NotificationLanguage language = user.getNotificationLanguage() != null ? NotificationLanguage.fromValue(user.getNotificationLanguage()) : defaultLanguage();
        StaffBotSettingsEntity settings = settingsService.getEntity();
        return new BotContext(chatId, callbackQueryId, messageId, user, language, settings, LocalDate.now(clock), action);
    }

    private NotificationLanguage defaultLanguage() {
        return NotificationLanguage.fromValue(notificationSettingsService.getEntity().getDefaultLanguage());
    }

    /** {@code /start@MyBot code} is how Telegram writes a command addressed to a bot by name. */
    private static String commandOf(String text) {
        String first = text.split("\\s+", 2)[0];
        String bare = first.contains("@") ? first.substring(0, first.indexOf('@')) : first;
        return bare.toLowerCase(Locale.ROOT);
    }
}
