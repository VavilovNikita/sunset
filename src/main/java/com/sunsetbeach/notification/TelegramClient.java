package com.sunsetbeach.notification;

import java.util.List;

/**
 * The few Telegram Bot API calls the notification centre needs. An interface so tests (and, one day,
 * another channel's adapter) never touch the network. Every failure is a {@link TelegramException}
 * whose message is in our own words - never Telegram's raw answer, never the bot token.
 */
public interface TelegramClient {

    /** False while the server has no {@code TELEGRAM_BOT_TOKEN}: nothing is polled or sent. */
    boolean isConfigured();

    /** The bot's username (no leading {@code @}) from {@code getMe}. */
    String getBotUsername() throws TelegramException;

    /**
     * {@code getUpdates} from {@code offset} (the id after the last one handled), waiting at most
     * {@code timeoutSeconds} for a new one. Only private-chat text messages are returned.
     */
    List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) throws TelegramException;

    /** Plain text, no parse mode: names and room labels need no escaping. */
    void sendMessage(String chatId, String text) throws TelegramException;

    /** One incoming private text message. */
    record TelegramUpdate(long updateId, String chatId, String text) {
    }

    class TelegramException extends Exception {

        public enum Kind {
            /** The user blocked the bot (HTTP 403): the link must be dropped. */
            BLOCKED,
            /** Telegram refused the request for good (HTTP 400, 401, 404): retrying changes nothing. */
            REJECTED,
            /** Rate limit, network failure or a Telegram outage: worth another try. */
            UNAVAILABLE
        }

        private final Kind kind;

        public TelegramException(Kind kind, String message) {
            super(message);
            this.kind = kind;
        }

        public Kind kind() {
            return kind;
        }
    }
}
