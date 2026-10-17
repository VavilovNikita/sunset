package com.sunsetbeach.notification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Telegram's Bot API over {@code java.net.http}, by long-ish polling ({@code getUpdates}) - no
 * webhook, so nothing public listens and it works from a developer machine.
 *
 * <p><b>The bot token is in every request URL, so nothing here ever logs a URL, a request or an
 * exception message.</b> A failure is logged by exception class and Telegram's numeric status only,
 * and the {@link TelegramException} carries a message in our own words. The token is never part of
 * anything returned.
 */
@Component
public class HttpTelegramClient implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(HttpTelegramClient.class);

    private static final int MAX_BODY_BYTES = 1 << 20;

    private final String token;
    private final String apiBase;
    private final HttpClient http;
    private final JsonMapper json;

    @Autowired
    public HttpTelegramClient(
            @Value("${app.notifications.telegram.bot-token:}") String token,
            @Value("${app.notifications.telegram.api-base:https://api.telegram.org}") String apiBase,
            JsonMapper json) {
        this.token = token == null ? "" : token.trim();
        this.apiBase = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public boolean isConfigured() {
        return !token.isEmpty();
    }

    @Override
    public String getBotUsername() throws TelegramException {
        JsonNode result = call("getMe", "{}", Duration.ofSeconds(10));
        String username = result.path("username").asString("");
        if (username.isBlank()) {
            throw new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not tell the bot's name");
        }
        return username;
    }

    @Override
    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) throws TelegramException {
        String body = "{\"offset\":" + offset + ",\"timeout\":" + timeoutSeconds + ",\"allowed_updates\":[\"message\"]}";
        JsonNode result = call("getUpdates", body, Duration.ofSeconds(timeoutSeconds + 10L));
        List<TelegramUpdate> updates = new ArrayList<>();
        for (JsonNode update : result) {
            long id = update.path("update_id").asLong(-1);
            if (id < 0) {
                continue;
            }
            JsonNode message = update.path("message");
            JsonNode chat = message.path("chat");
            String text = message.path("text").isString() ? message.path("text").asString() : null;
            // A group or channel is never a person's own chat; the offset still moves past it.
            if (text == null || !"private".equals(chat.path("type").asString("")) || !chat.path("id").isNumber()) {
                updates.add(new TelegramUpdate(id, null, null));
                continue;
            }
            updates.add(new TelegramUpdate(id, String.valueOf(chat.path("id").asLong()), text));
        }
        return updates;
    }

    @Override
    public void sendMessage(String chatId, String text) throws TelegramException {
        String body = json.createObjectNode().put("chat_id", chatId).put("text", text).put("disable_web_page_preview", true).toString();
        call("sendMessage", body, Duration.ofSeconds(15));
    }

    private JsonNode call(String method, String body, Duration timeout) throws TelegramException {
        if (!isConfigured()) {
            throw new TelegramException(TelegramException.Kind.REJECTED, "Telegram is not configured on the server");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/bot" + token + "/" + method))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram request was interrupted");
        } catch (IOException | RuntimeException e) {
            // Class name only: the message of a connect failure can contain the URL, and the URL has the token.
            log.warn("Telegram {} failed: {}", method, e.getClass().getSimpleName());
            throw new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        }
        int status = response.statusCode();
        byte[] raw = response.body() == null ? new byte[0] : response.body();
        JsonNode parsed = null;
        if (raw.length > 0 && raw.length <= MAX_BODY_BYTES) {
            try {
                parsed = json.readTree(raw);
            } catch (RuntimeException e) {
                parsed = null;
            }
        }
        if (status == 200 && parsed != null && parsed.path("ok").asBoolean(false)) {
            return parsed.path("result");
        }
        log.warn("Telegram {} answered HTTP {}", method, status);
        if (status == 403) {
            throw new TelegramException(TelegramException.Kind.BLOCKED, "The recipient blocked the bot");
        }
        if (status == 429 || status >= 500) {
            throw new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram is busy or unavailable (HTTP " + status + ")");
        }
        throw new TelegramException(TelegramException.Kind.REJECTED, "Telegram refused the request (HTTP " + status + ")");
    }
}
