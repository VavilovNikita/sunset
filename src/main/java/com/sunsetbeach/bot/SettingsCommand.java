package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationAction;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.service.AuditLogService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bot's own settings for one person: the language of messages, the evening-before reminder on/off and, for a
 * therapist, the morning message about their procedures on/off.
 * Each is the employee's own choice - audited as them (via Telegram), and an unchanged press writes nothing.
 */
@Component
public class SettingsCommand implements BotCommandHandler, BotActionHandler {

    private static final Map<NotificationLanguage, String> LANGUAGE_NAMES = Map.of(
            NotificationLanguage.EN, "English", NotificationLanguage.TH, "ไทย", NotificationLanguage.RU, "Русский");

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SettingsCommand(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public String command() {
        return "settings";
    }

    @Override
    public Key label() {
        return Key.MENU_SETTINGS;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isBotSettingsEnabled();
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        return List.of(screen(ctx, ctx.user(), ctx.language(), false));
    }

    @Override
    public Set<String> kinds() {
        return Set.of(NotificationAction.Kinds.SET_LANGUAGE, NotificationAction.Kinds.SET_EVE_REMINDER, NotificationAction.Kinds.SET_THERAPIST_MORNING);
    }

    @Override
    @Transactional
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        if (!ctx.settings().isBotSettingsEnabled()) {
            throw new ForbiddenException("Switched off");
        }
        UserEntity user = userRepository.findById(ctx.user().getId()).orElseThrow();
        if (!user.isActive()) {
            throw new ForbiddenException("Inactive");
        }
        NotificationLanguage language = ctx.language();
        Key confirmation = null;
        if (NotificationAction.Kinds.SET_LANGUAGE.equals(action.getKind())) {
            NotificationLanguage chosen = NotificationLanguage.fromValue(action.getArg());
            String before = user.getNotificationLanguage();
            if (!chosen.getValue().equals(before)) {
                user.setNotificationLanguage(chosen.getValue());
                userRepository.saveAndFlush(user);
                audit(user, AuditAction.NOTIFICATION_LANGUAGE_UPDATED, "Notification language of " + user.getName() + ": "
                        + (before == null ? "default" : before) + " -> " + chosen.getValue());
            }
            language = chosen;
            confirmation = Key.LANG_CHANGED;
        } else if (NotificationAction.Kinds.SET_THERAPIST_MORNING.equals(action.getKind())) {
            // Only a therapist has this message, and only while the feature is on; a press from anyone else changes nothing.
            if (BotAccess.hasFunction(user, "THERAPIST") && ctx.settings().isTherapistMorningEnabled()) {
                boolean on = "on".equals(action.getArg());
                if (user.getTherapistMorningEnabled() == null || user.getTherapistMorningEnabled() != on) {
                    user.setTherapistMorningEnabled(on);
                    userRepository.saveAndFlush(user);
                    audit(user, AuditAction.STAFF_THERAPIST_MORNING_CHANGED, "Morning procedures message of " + user.getName() + " " + (on ? "on" : "off"));
                }
            }
        } else if (ctx.settings().isEveReminderEnabled()) {
            boolean on = "on".equals(action.getArg());
            if (user.getEveReminderEnabled() == null || user.getEveReminderEnabled() != on) {
                user.setEveReminderEnabled(on);
                userRepository.saveAndFlush(user);
                audit(user, AuditAction.STAFF_EVE_REMINDER_CHANGED, "Evening-before reminder of " + user.getName() + " " + (on ? "on" : "off"));
            }
        }
        BotReply screen = screen(ctx, user, language, true);
        String toast = confirmation == null ? null : NotificationMessages.render(confirmation, language, Map.of());
        return List.of(toast == null ? screen : screen.toast(toast));
    }

    private BotReply screen(BotContext ctx, UserEntity user, NotificationLanguage language, boolean edit) {
        boolean eve = effectiveEve(user, ctx.settings());
        boolean therapist = BotAccess.hasFunction(user, "THERAPIST") && ctx.settings().isTherapistMorningEnabled();
        boolean morning = effectiveMorning(user, ctx.settings());
        String text = NotificationMessages.render(Key.SETTINGS_TEXT, language, Map.of(
                "language", LANGUAGE_NAMES.get(language),
                "eve", NotificationMessages.render(eve ? Key.WORD_ON : Key.WORD_OFF, language, Map.of())));
        if (therapist) {
            text += "\n" + NotificationMessages.render(Key.SETTINGS_THERAPIST, language, Map.of(
                    "morning", NotificationMessages.render(morning ? Key.WORD_ON : Key.WORD_OFF, language, Map.of())));
        }
        BotReply reply = BotReply.text(text);
        reply.row(LANGUAGE_NAMES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new BotReply.Button(e.getValue(), NotificationAction.Kinds.SET_LANGUAGE, null, e.getKey().getValue()))
                .toList());
        if (ctx.settings().isEveReminderEnabled()) {
            reply.row(new BotReply.Button(
                    NotificationMessages.render(eve ? Key.BTN_EVE_OFF : Key.BTN_EVE_ON, language, Map.of()),
                    NotificationAction.Kinds.SET_EVE_REMINDER, null, eve ? "off" : "on"));
        }
        if (therapist) {
            reply.row(new BotReply.Button(
                    NotificationMessages.render(morning ? Key.BTN_MORNING_OFF : Key.BTN_MORNING_ON, language, Map.of()),
                    NotificationAction.Kinds.SET_THERAPIST_MORNING, null, morning ? "off" : "on"));
        }
        return edit ? reply.editCurrent() : reply;
    }

    private static boolean effectiveMorning(UserEntity user, StaffBotSettingsEntity settings) {
        return user.getTherapistMorningEnabled() != null ? user.getTherapistMorningEnabled() : settings.isTherapistMorningDefaultOn();
    }

    private static boolean effectiveEve(UserEntity user, StaffBotSettingsEntity settings) {
        return user.getEveReminderEnabled() != null ? user.getEveReminderEnabled() : settings.isEveReminderDefaultOn();
    }

    private void audit(UserEntity user, AuditAction action, String summary) {
        auditLogService.recordAs(new AuditLogService.Actor(user.getId(), user.getEmail() == null ? user.getName() : user.getEmail(), user.getRole(), "Telegram"),
                action, AuditEntityType.USER, user.getId(), summary);
    }
}
