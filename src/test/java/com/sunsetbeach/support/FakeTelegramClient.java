package com.sunsetbeach.support;

import com.sunsetbeach.notification.TelegramClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * A Telegram that never touches the network: it records what was sent (with its buttons), edited and
 * answered, can be told to fail a send (by chat id) and hands out queued incoming updates.
 */
public class FakeTelegramClient implements TelegramClient {

    public record Sent(String chatId, String text, String messageId, List<List<Button>> buttons) {
    }

    public record Edit(String chatId, String messageId, String text, List<List<Button>> buttons) {
    }

    /** A reply keyboard (the menu under the input field) a message carried. */
    public record Keyboard(String chatId, String messageText, List<List<String>> rows) {
    }

    /** What one setMyCommands call set. */
    public record CommandList(String chatId, String languageCode, List<Command> commands) {
    }

    public volatile boolean configured = true;
    public volatile Route route = Route.DIRECT;
    public volatile boolean proxyConfigured = false;
    public volatile String botUsername = "sunset_test_bot";
    public final List<Sent> sent = new CopyOnWriteArrayList<>();
    public final List<Edit> edits = new CopyOnWriteArrayList<>();
    public final List<String> answers = new CopyOnWriteArrayList<>();
    public final List<String> commandSets = new CopyOnWriteArrayList<>();
    public final List<Keyboard> keyboards = new CopyOnWriteArrayList<>();
    public final List<CommandList> commandLists = new CopyOnWriteArrayList<>();
    public final List<TelegramUpdate> incoming = new CopyOnWriteArrayList<>();
    /** Returns the failure for a chat, or null to let the send succeed. */
    public volatile Function<String, TelegramException> failure = chat -> null;
    public volatile long lastRequestedOffset = -1;
    /** When set, getUpdates throws it (Telegram unreachable); getUpdatesCalls counts every try. */
    public volatile TelegramException updatesFailure;
    public final java.util.concurrent.atomic.AtomicInteger getUpdatesCalls = new java.util.concurrent.atomic.AtomicInteger();
    /** When set, answering a button press fails with it. */
    public volatile TelegramException answerFailure;
    /** Runs at the moment a press is answered, so a test can see what had (not) been written by then. */
    public volatile Runnable onAnswer = () -> { };
    private final AtomicLong messageIds = new AtomicLong(1000);

    public void reset() {
        configured = true;
        route = Route.DIRECT;
        proxyConfigured = false;
        botUsername = "sunset_test_bot";
        sent.clear();
        edits.clear();
        answers.clear();
        commandSets.clear();
        keyboards.clear();
        commandLists.clear();
        incoming.clear();
        failure = chat -> null;
        lastRequestedOffset = -1;
        updatesFailure = null;
        getUpdatesCalls.set(0);
        answerFailure = null;
        onAnswer = () -> { };
    }

    public List<String> textsTo(String chatId) {
        return sent.stream().filter(s -> s.chatId().equals(chatId)).map(Sent::text).toList();
    }

    /** The newest message sent to a chat. */
    public Sent lastTo(String chatId) {
        List<Sent> all = sent.stream().filter(s -> s.chatId().equals(chatId)).toList();
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    /** The callback data of the button with this label under a message (null if none). */
    public static String callbackOf(Sent message, String label) {
        if (message == null || message.buttons() == null) {
            return null;
        }
        return message.buttons().stream().flatMap(List::stream).filter(b -> b.text().equals(label)).map(Button::callbackData).findFirst().orElse(null);
    }

    @Override
    public Route route() {
        return route;
    }

    @Override
    public boolean proxyConfigured() {
        return proxyConfigured;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) throws TelegramException {
        getUpdatesCalls.incrementAndGet();
        if (updatesFailure != null) {
            throw updatesFailure;
        }
        lastRequestedOffset = offset;
        List<TelegramUpdate> now = new ArrayList<>(incoming.stream().filter(u -> u.updateId() >= offset).toList());
        incoming.clear();
        return now;
    }

    @Override
    public String sendMessage(String chatId, String text, List<List<Button>> inlineKeyboard, List<List<String>> replyKeyboard) throws TelegramException {
        TelegramException problem = failure.apply(chatId);
        if (problem != null) {
            throw problem;
        }
        String id = String.valueOf(messageIds.incrementAndGet());
        sent.add(new Sent(chatId, text, id, inlineKeyboard));
        if (replyKeyboard != null && !replyKeyboard.isEmpty()) {
            keyboards.add(new Keyboard(chatId, text, replyKeyboard));
        }
        return id;
    }

    @Override
    public void editMessageText(String chatId, String messageId, String text, List<List<Button>> inlineKeyboard) {
        edits.add(new Edit(chatId, messageId, text, inlineKeyboard));
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text) throws TelegramException {
        onAnswer.run();
        if (answerFailure != null) {
            throw answerFailure;
        }
        answers.add(text == null ? "" : text);
    }

    @Override
    public void setMyCommands(List<Command> commands, String languageCode, String chatId) {
        commandLists.add(new CommandList(chatId, languageCode, List.copyOf(commands)));
        commandSets.add((chatId == null ? "default" : chatId) + ":" + (languageCode == null ? "-" : languageCode) + ":" + commands.size());
    }
}
