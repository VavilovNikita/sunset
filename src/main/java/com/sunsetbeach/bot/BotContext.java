package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import java.time.LocalDate;
import java.util.Map;

/**
 * Who is talking and in what state, handed to every handler. Built fresh for each update from the database
 * (the person, their language, the current settings, the hotel's today), so a handler never works from
 * anything stale. {@code action} is the pressed button's row for action handlers, null otherwise.
 */
public record BotContext(
        String chatId,
        String callbackQueryId,
        String messageId,
        UserEntity user,
        NotificationLanguage language,
        StaffBotSettingsEntity settings,
        LocalDate today,
        StaffActionEntity action) {

    public String t(Key key) {
        return NotificationMessages.render(key, language, Map.of());
    }

    public String t(Key key, Map<String, String> params) {
        return NotificationMessages.render(key, language, params);
    }

    public BotContext withAction(StaffActionEntity pressed) {
        return new BotContext(chatId, callbackQueryId, messageId, user, language, settings, today, pressed);
    }
}
