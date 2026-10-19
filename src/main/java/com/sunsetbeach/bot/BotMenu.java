package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.notification.TelegramClient.Command;
import com.sunsetbeach.service.NotificationSettingsService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * What one person's menu is: the screens they may use now, as the keyboard under the input field and as their
 * "/" list, in their language. One place, so the bot's reply, the refresh after an administrator's change and
 * the {@link #signature} that says whether the keyboard on the person's phone is still right can't disagree.
 */
@Component
public class BotMenu {

    private final List<BotCommandHandler> handlers;
    private final NotificationSettingsService notificationSettings;

    public BotMenu(List<BotCommandHandler> handlers, NotificationSettingsService notificationSettings) {
        this.handlers = handlers.stream().sorted(Comparator.comparingInt(BotCommandHandler::order)).toList();
        this.notificationSettings = notificationSettings;
    }

    /** The person's language, or the hotel's default while none is set. */
    public NotificationLanguage languageOf(UserEntity user) {
        return user.getNotificationLanguage() != null
                ? NotificationLanguage.fromValue(user.getNotificationLanguage())
                : NotificationLanguage.fromValue(notificationSettings.getEntity().getDefaultLanguage());
    }

    public List<BotCommandHandler> visible(UserEntity user, StaffBotSettingsEntity settings, LocalDate today) {
        return handlers.stream().filter(h -> h.enabled(settings) && h.availableTo(user, today)).toList();
    }

    public List<List<String>> keyboard(List<BotCommandHandler> visible, NotificationLanguage language) {
        List<List<String>> keyboard = new ArrayList<>();
        List<String> row = new ArrayList<>();
        for (BotCommandHandler h : visible) {
            row.add(label(h, language));
            if (row.size() == 2) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            keyboard.add(row);
        }
        return keyboard;
    }

    /** This person's own "/" list: the menu plus what only they may use. */
    public List<Command> commands(List<BotCommandHandler> visible, NotificationLanguage language) {
        List<Command> commands = new ArrayList<>();
        commands.add(new Command("menu", NotificationMessages.render(Key.BOT_MENU, language, Map.of())));
        visible.forEach(h -> commands.add(new Command(h.command(), label(h, language))));
        return commands;
    }

    /** Equal exactly when the keyboard shows the same buttons in the same order. */
    public String signature(List<BotCommandHandler> visible, NotificationLanguage language) {
        StringBuilder sb = new StringBuilder(language.getValue());
        visible.forEach(h -> sb.append('|').append(label(h, language)));
        return sb.toString();
    }

    private static String label(BotCommandHandler handler, NotificationLanguage language) {
        return NotificationMessages.render(handler.label(), language, Map.of());
    }
}
