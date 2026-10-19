package com.sunsetbeach.service;

import com.sunsetbeach.bot.BotMenuChanged;
import com.sunsetbeach.entity.ChannelLinkCodeEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.TelegramBotStateEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.NotificationChannel;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationLinkCode;
import com.sunsetbeach.model.NotificationStaffMember;
import com.sunsetbeach.model.NotificationStatus;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.repository.ChannelLinkCodeRepository;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.TelegramBotStateRepository;
import com.sunsetbeach.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Who is reachable where: the one-time link codes, the links themselves, the bot's conversation
 * ({@code /start <code>}, {@code /stop}) and the staff list the Link Telegram button needs.
 *
 * <p>A link needs no login on the employee's side - many have neither email nor password. A manager
 * creates a code (15 minutes, single use), the employee opens {@code t.me/<bot>?start=<code>} and
 * presses Start, and the bot ties that chat to them. One chat belongs to at most one employee and one
 * employee has at most one chat per channel; both are unique indexes, so even two chats opening one
 * code at the same moment end with one link.
 */
@Service
public class NotificationChannelService {

    private static final Logger log = LoggerFactory.getLogger(NotificationChannelService.class);

    static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
    private static final Pattern CODE_SHAPE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final String TELEGRAM = "TELEGRAM";

    private final UserRepository userRepository;
    private final StaffChannelLinkRepository linkRepository;
    private final ChannelLinkCodeRepository codeRepository;
    private final TelegramBotStateRepository botStateRepository;
    private final TelegramClient telegram;
    private final NotificationSettingsService settingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();
    private final TelegramHealth health;
    private final ApplicationEventPublisher events;

    public NotificationChannelService(
            UserRepository userRepository,
            StaffChannelLinkRepository linkRepository,
            ChannelLinkCodeRepository codeRepository,
            TelegramBotStateRepository botStateRepository,
            TelegramClient telegram,
            NotificationSettingsService settingsService,
            AuditLogService auditLogService,
            Clock clock,
            PlatformTransactionManager transactionManager,
            TelegramHealth health,
            ApplicationEventPublisher events) {
        this.health = health;
        this.events = events;
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.codeRepository = codeRepository;
        this.botStateRepository = botStateRepository;
        this.telegram = telegram;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ---- status -----------------------------------------------------------------------------

    /** From configuration, the cached getMe answer and the poll loop's own record ({@link TelegramHealth}) - never calls Telegram. */
    @Transactional(readOnly = true)
    public NotificationStatus status() {
        TelegramHealth.Snapshot health = this.health.snapshot();
        NotificationStatus status = new NotificationStatus(telegram.isConfigured(), telegram.isConfigured() ? health.consecutiveFailures() : 0,
                NotificationStatus.TelegramRouteEnum.valueOf(telegram.route().name()), telegram.proxyConfigured());
        if (telegram.isConfigured()) {
            botStateRepository.findById(1).map(TelegramBotStateEntity::getBotUsername).ifPresent(status::botUsername);
            // Absent (not null) while unknown, like botUsername.
            if (health.lastSuccessAt() != null) {
                status.lastSuccessAt(health.lastSuccessAt().atZone(clock.getZone()).toOffsetDateTime());
            }
            if (health.failingSince() != null) {
                status.failingSince(health.failingSince().atZone(clock.getZone()).toOffsetDateTime());
            }
        }
        return status;
    }

    /** Asks Telegram for the bot's name and caches it. Returns it, or empty when Telegram can't say. */
    public Optional<String> refreshBotUsername() {
        if (!telegram.isConfigured()) {
            return Optional.empty();
        }
        try {
            String username = telegram.getBotUsername();
            tx.executeWithoutResult(s -> {
                TelegramBotStateEntity state = botStateRepository.findById(1).orElseThrow();
                state.setBotUsername(username);
                botStateRepository.save(state);
            });
            return Optional.of(username);
        } catch (TelegramException e) {
            log.warn("Could not read the bot's name from Telegram: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> cachedBotUsername() {
        return botStateRepository.findById(1).map(TelegramBotStateEntity::getBotUsername);
    }

    // ---- staff list, language -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationStaffMember> listStaff() {
        List<UserEntity> users = userRepository.findAll();
        Map<String, StaffChannelLinkEntity> telegramLinks = linkRepository.findByUserIdIn(users.stream().map(UserEntity::getId).toList()).stream()
                .filter(l -> TELEGRAM.equals(l.getChannel()))
                .collect(Collectors.toMap(StaffChannelLinkEntity::getUserId, l -> l));
        return users.stream()
                .sorted(Comparator.comparing(UserEntity::isActive).reversed().thenComparing(u -> u.getName().toLowerCase()))
                .map(u -> toMember(u, telegramLinks.get(u.getId())))
                .toList();
    }

    @Transactional
    public NotificationStaffMember setLanguage(String userId, NotificationLanguage language) {
        UserEntity user = requireUser(userId);
        String before = user.getNotificationLanguage();
        user.setNotificationLanguage(language == null ? null : language.getValue());
        userRepository.saveAndFlush(user);
        if (!java.util.Objects.equals(before, user.getNotificationLanguage())) {
            auditLogService.record(
                    AuditAction.NOTIFICATION_LANGUAGE_UPDATED,
                    AuditEntityType.USER,
                    userId,
                    "Notification language of " + user.getName() + ": " + (before == null ? "default" : before) + " -> "
                            + (user.getNotificationLanguage() == null ? "default" : user.getNotificationLanguage()));
            // The "/" list of this person's chat is in their language: renewed after the commit, off this thread.
            events.publishEvent(new BotMenuChanged(userId));
        }
        return toMember(user, linkRepository.findByUserIdAndChannel(userId, TELEGRAM).orElse(null));
    }

    private static NotificationStaffMember toMember(UserEntity u, StaffChannelLinkEntity link) {
        NotificationStaffMember member = new NotificationStaffMember(u.getId(), u.getName(), u.getRole(), u.isActive(), link != null);
        if (u.getNotificationLanguage() != null) {
            member.language(NotificationLanguage.fromValue(u.getNotificationLanguage()));
        }
        if (link != null) {
            member.telegramLinkedAt(TimestampFormat.toUtc(link.getCreatedAt()));
        }
        return member;
    }

    // ---- link codes -----------------------------------------------------------------------------

    /** MANAGER+ pressed "Link Telegram" for an employee. */
    public NotificationLinkCode createLinkCode(String userId, NotificationChannel channel) {
        if (!telegram.isConfigured()) {
            throw new ConflictException("Telegram is not configured on the server (no TELEGRAM_BOT_TOKEN)");
        }
        UserEntity user = requireUser(userId);
        if (!user.isActive()) {
            throw new ConflictException(user.getName() + " is deactivated");
        }
        String bot = cachedBotUsername().or(this::refreshBotUsername)
                .orElseThrow(() -> new ConflictException("The Telegram bot is not reachable yet - try again in a moment"));

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 32 characters of A-Za-z0-9_-
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        LocalDateTime expiresAt = nowUtc.plus(CODE_LIFETIME);
        String actorId = currentActorId();
        tx.executeWithoutResult(s -> {
            // An earlier code of this person stops working: only the newest link can be used.
            codeRepository.deleteUnusedFor(userId, TELEGRAM);
            codeRepository.deleteExpiredBefore(nowUtc.minusDays(1));
            ChannelLinkCodeEntity entity = new ChannelLinkCodeEntity();
            entity.setCode(code);
            entity.setUserId(userId);
            entity.setChannel(TELEGRAM);
            entity.setCreatedByUserId(actorId);
            entity.setExpiresAt(expiresAt);
            codeRepository.save(entity);
        });
        // The code itself is never written to the audit log.
        auditLogService.record(
                AuditAction.NOTIFICATION_LINK_CODE_CREATED, AuditEntityType.USER, userId, "Telegram link code created for " + user.getName());
        return new NotificationLinkCode(userId, "https://t.me/" + bot + "?start=" + code, TimestampFormat.toUtc(expiresAt));
    }

    private static String currentActorId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof com.sunsetbeach.security.StaffPrincipal p ? p.id() : null;
    }

    // ---- unlink -----------------------------------------------------------------------------------

    @Transactional
    public void unlink(String userId, NotificationChannel channel) {
        UserEntity user = requireUser(userId);
        String channelName = channel == null ? TELEGRAM : channel.getValue();
        StaffChannelLinkEntity link = linkRepository.findByUserIdAndChannel(userId, channelName)
                .orElseThrow(() -> new NotFoundException(user.getName() + " has no linked " + channelName.toLowerCase() + " chat"));
        linkRepository.delete(link);
        auditLogService.record(AuditAction.NOTIFICATION_CHANNEL_UNLINKED, AuditEntityType.USER, userId, "Telegram unlinked from " + user.getName());
    }

    /** A deactivated employee no longer receives anything: their link goes with the deactivation (same transaction). */
    public void onUserDeactivated(UserEntity user) {
        List<StaffChannelLinkEntity> links = linkRepository.findByUserId(user.getId());
        if (links.isEmpty()) {
            return;
        }
        linkRepository.deleteAll(links);
        auditLogService.record(
                AuditAction.NOTIFICATION_CHANNEL_UNLINKED, AuditEntityType.USER, user.getId(), "Telegram unlinked from " + user.getName() + " (deactivated)");
    }

    private UserEntity requireUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    // ---- the bot's conversation ---------------------------------------------------------------------

    /** Handles one incoming private message and answers it. Never throws. */
    public void handleUpdate(TelegramUpdate update) {
        if (update.chatId() == null || update.text() == null) {
            return;
        }
        String reply;
        try {
            reply = replyTo(update.chatId(), update.text().strip());
        } catch (RuntimeException e) {
            log.error("Handling a Telegram message failed", e);
            return;
        }
        if (reply == null) {
            return;
        }
        try {
            telegram.sendMessage(update.chatId(), reply);
        } catch (TelegramException e) {
            log.warn("Could not answer a Telegram chat: {}", e.getMessage());
        }
    }

    private String replyTo(String chatId, String text) {
        NotificationLanguage chatLanguage = linkRepository.findByChannelAndExternalId(TELEGRAM, chatId)
                .flatMap(l -> userRepository.findById(l.getUserId()))
                .map(this::languageOf)
                .orElseGet(this::defaultLanguage);
        String command = text.split("\\s+", 2)[0];
        String argument = text.contains(" ") ? text.substring(text.indexOf(' ')).strip() : "";
        // "/start@MyBot code" is how Telegram writes a command in a group-style mention.
        String bare = command.contains("@") ? command.substring(0, command.indexOf('@')) : command;

        if (bare.equalsIgnoreCase("/stop")) {
            return stop(chatId, chatLanguage);
        }
        if (bare.equalsIgnoreCase("/start") && !argument.isEmpty()) {
            return link(chatId, argument);
        }
        return NotificationMessages.render(NotificationMessages.Key.BOT_HELP, chatLanguage, Map.of());
    }

    private String stop(String chatId, NotificationLanguage language) {
        Optional<StaffChannelLinkEntity> link = linkRepository.findByChannelAndExternalId(TELEGRAM, chatId);
        if (link.isEmpty()) {
            return NotificationMessages.render(NotificationMessages.Key.BOT_NOT_LINKED, language, Map.of());
        }
        tx.executeWithoutResult(s -> linkRepository.delete(link.get()));
        auditLogService.recordSystemAction(
                AuditAction.NOTIFICATION_CHANNEL_UNLINKED, AuditEntityType.USER, link.get().getUserId(), "Telegram unlinked by the employee (/stop)");
        return NotificationMessages.render(NotificationMessages.Key.BOT_UNLINKED, language, Map.of());
    }

    private String link(String chatId, String code) {
        NotificationLanguage fallback = defaultLanguage();
        if (!CODE_SHAPE.matcher(code).matches()) {
            return NotificationMessages.render(NotificationMessages.Key.BOT_LINK_INVALID, fallback, Map.of());
        }
        try {
            // One transaction: a chat that turns out to be taken rolls the spent code back, so the link stays usable.
            UserEntity linked = tx.execute(s -> {
                ChannelLinkCodeEntity entity = codeRepository.findById(code).orElse(null);
                if (entity == null || !TELEGRAM.equals(entity.getChannel())) {
                    return null;
                }
                Optional<StaffChannelLinkEntity> chatOwner = linkRepository.findByChannelAndExternalId(TELEGRAM, chatId);
                if (chatOwner.isPresent() && !chatOwner.get().getUserId().equals(entity.getUserId())) {
                    throw new ChatTaken();
                }
                if (codeRepository.consume(code, TimestampFormat.nowUtc(clock)) == 0) {
                    return null;
                }
                UserEntity user = userRepository.findById(entity.getUserId()).orElse(null);
                if (user == null || !user.isActive()) {
                    return null;
                }
                StaffChannelLinkEntity link = linkRepository.findByUserIdAndChannel(user.getId(), TELEGRAM).orElseGet(StaffChannelLinkEntity::new);
                link.setUserId(user.getId());
                link.setChannel(TELEGRAM);
                link.setExternalId(chatId);
                linkRepository.saveAndFlush(link);
                return user;
            });
            if (linked == null) {
                return NotificationMessages.render(NotificationMessages.Key.BOT_LINK_INVALID, fallback, Map.of());
            }
            auditLogService.recordSystemAction(
                    AuditAction.NOTIFICATION_CHANNEL_LINKED, AuditEntityType.USER, linked.getId(), "Telegram linked to " + linked.getName());
            return NotificationMessages.render(NotificationMessages.Key.BOT_LINKED, languageOf(linked), Map.of("name", linked.getName()));
        } catch (ChatTaken | DataIntegrityViolationException e) {
            return NotificationMessages.render(NotificationMessages.Key.BOT_CHAT_TAKEN, fallback, Map.of());
        }
    }

    private static final class ChatTaken extends RuntimeException {
        ChatTaken() {
            super(null, null, false, false);
        }
    }

    private NotificationLanguage languageOf(UserEntity user) {
        return user.getNotificationLanguage() != null ? NotificationLanguage.fromValue(user.getNotificationLanguage()) : defaultLanguage();
    }

    private NotificationLanguage defaultLanguage() {
        return NotificationLanguage.fromValue(settingsService.getEntity().getDefaultLanguage());
    }
}
