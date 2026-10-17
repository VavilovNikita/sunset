package com.sunsetbeach.support;

import com.sunsetbeach.notification.TelegramClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A Telegram that never touches the network: it records what was sent, can be told to fail a send
 * (by chat id) and hands out queued incoming updates.
 */
public class FakeTelegramClient implements TelegramClient {

    public record Sent(String chatId, String text) {
    }

    public volatile boolean configured = true;
    public volatile String botUsername = "sunset_test_bot";
    public final List<Sent> sent = new CopyOnWriteArrayList<>();
    public final List<TelegramUpdate> incoming = new CopyOnWriteArrayList<>();
    /** Returns the failure for a chat, or null to let the send succeed. */
    public volatile Function<String, TelegramException> failure = chat -> null;
    public volatile long lastRequestedOffset = -1;

    public void reset() {
        configured = true;
        botUsername = "sunset_test_bot";
        sent.clear();
        incoming.clear();
        failure = chat -> null;
        lastRequestedOffset = -1;
    }

    public List<String> textsTo(String chatId) {
        return sent.stream().filter(s -> s.chatId().equals(chatId)).map(Sent::text).toList();
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
    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) {
        lastRequestedOffset = offset;
        List<TelegramUpdate> now = new ArrayList<>(incoming.stream().filter(u -> u.updateId() >= offset).toList());
        incoming.clear();
        return now;
    }

    @Override
    public void sendMessage(String chatId, String text) throws TelegramException {
        TelegramException problem = failure.apply(chatId);
        if (problem != null) {
            throw problem;
        }
        sent.add(new Sent(chatId, text));
    }
}
