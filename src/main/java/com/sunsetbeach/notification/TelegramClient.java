package com.sunsetbeach.notification;

import java.util.List;

/**
 * The few Telegram Bot API calls the notification centre and the staff bot need. An interface so tests
 * (and, one day, another channel's adapter) never touch the network. Every failure is a
 * {@link TelegramException} whose message is in our own words - never Telegram's raw answer, never the
 * bot token.
 */
public interface TelegramClient {

    /** False while the server has no {@code TELEGRAM_BOT_TOKEN}: nothing is polled or sent. */
    boolean isConfigured();

    /** Which way requests go to Telegram right now. Always {@link Route#DIRECT} for a client without a proxy. */
    default Route route() {
        return Route.DIRECT;
    }

    /** Whether a proxy is configured (its address is never exposed). */
    default boolean proxyConfigured() {
        return false;
    }

    /** The bot's username (no leading {@code @}) from {@code getMe}. */
    String getBotUsername() throws TelegramException;

    /**
     * {@code getUpdates} from {@code offset} (the id after the last one handled), waiting at most
     * {@code timeoutSeconds} for a new one. Only private-chat text messages and button presses
     * ({@code callback_query}) are returned.
     */
    List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) throws TelegramException;

    /**
     * Plain text, no parse mode: names and room labels need no escaping. At most one of the keyboards is
     * given: {@code inlineKeyboard} (buttons under the message, each carrying opaque callback data) or
     * {@code replyKeyboard} (the persistent menu under the input field; its buttons just send their text).
     *
     * @return the id of the message Telegram created, so it can be edited later
     */
    String sendMessage(String chatId, String text, List<List<Button>> inlineKeyboard, List<List<String>> replyKeyboard) throws TelegramException;

    default void sendMessage(String chatId, String text) throws TelegramException {
        sendMessage(chatId, text, null, null);
    }

    /** Replaces a message's text; {@code inlineKeyboard} null or empty removes the buttons. */
    void editMessageText(String chatId, String messageId, String text, List<List<Button>> inlineKeyboard) throws TelegramException;

    /** Stops the spinner on a pressed button; {@code text} (nullable) is shown to the person briefly. */
    void answerCallbackQuery(String callbackQueryId, String text) throws TelegramException;

    /**
     * {@code setMyCommands}: the "/" menu. {@code chatId} null sets the default for everyone, otherwise only
     * that chat; {@code languageCode} (nullable) limits it to Telegram clients in that language.
     */
    void setMyCommands(List<Command> commands, String languageCode, String chatId) throws TelegramException;

    /** A button under a message. {@code callbackData} is at most 64 bytes - in practice a short opaque id. */
    record Button(String text, String callbackData) {
    }

    /** The way a request reaches Telegram: straight from this server, or through the configured HTTP proxy. */
    enum Route {
        DIRECT,
        PROXY
    }

    record Command(String command, String description) {
    }

    /**
     * One incoming update: a private text message ({@code text}) or a button press
     * ({@code callbackQueryId} and {@code callbackData} set, {@code messageId} the message it sat under).
     * An update that is neither (a group message, a photo) has a null {@code chatId}; the offset still moves past it.
     */
    record TelegramUpdate(long updateId, String chatId, String text, String callbackQueryId, String callbackData, String messageId) {

        public TelegramUpdate(long updateId, String chatId, String text) {
            this(updateId, chatId, text, null, null, null);
        }

        public boolean isCallback() {
            return callbackQueryId != null;
        }
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
