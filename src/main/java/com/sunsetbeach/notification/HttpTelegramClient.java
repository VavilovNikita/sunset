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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
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
        String body = "{\"offset\":" + offset + ",\"timeout\":" + timeoutSeconds + ",\"allowed_updates\":[\"message\",\"callback_query\"]}";
        JsonNode result = call("getUpdates", body, Duration.ofSeconds(timeoutSeconds + 10L));
        List<TelegramUpdate> updates = new ArrayList<>();
        for (JsonNode update : result) {
            long id = update.path("update_id").asLong(-1);
            if (id < 0) {
                continue;
            }
            JsonNode callback = update.path("callback_query");
            if (callback.isObject()) {
                JsonNode chat = callback.path("message").path("chat");
                String data = callback.path("data").isString() ? callback.path("data").asString() : null;
                String queryId = callback.path("id").isString() ? callback.path("id").asString() : null;
                if (data == null || queryId == null || !"private".equals(chat.path("type").asString("")) || !chat.path("id").isNumber()) {
                    updates.add(new TelegramUpdate(id, null, null));
                    continue;
                }
                String messageId = callback.path("message").path("message_id").isNumber()
                        ? String.valueOf(callback.path("message").path("message_id").asLong()) : null;
                updates.add(new TelegramUpdate(id, String.valueOf(chat.path("id").asLong()), null, queryId, data, messageId));
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
    public String sendMessage(String chatId, String text, List<List<Button>> inlineKeyboard, List<List<String>> replyKeyboard) throws TelegramException {
        ObjectNode body = json.createObjectNode().put("chat_id", chatId).put("text", text).put("disable_web_page_preview", true);
        if (inlineKeyboard != null && !inlineKeyboard.isEmpty()) {
            body.set("reply_markup", inlineMarkup(inlineKeyboard));
        } else if (replyKeyboard != null && !replyKeyboard.isEmpty()) {
            ObjectNode markup = json.createObjectNode();
            ArrayNode rows = markup.putArray("keyboard");
            for (List<String> row : replyKeyboard) {
                ArrayNode r = rows.addArray();
                row.forEach(label -> r.addObject().put("text", label));
            }
            markup.put("resize_keyboard", true).put("is_persistent", true);
            body.set("reply_markup", markup);
        }
        JsonNode result = call("sendMessage", body.toString(), Duration.ofSeconds(15));
        return result.path("message_id").isNumber() ? String.valueOf(result.path("message_id").asLong()) : null;
    }

    @Override
    public void editMessageText(String chatId, String messageId, String text, List<List<Button>> inlineKeyboard) throws TelegramException {
        ObjectNode body = json.createObjectNode().put("chat_id", chatId).put("text", text).put("disable_web_page_preview", true);
        try {
            body.put("message_id", Long.parseLong(messageId));
        } catch (NumberFormatException e) {
            throw new TelegramException(TelegramException.Kind.REJECTED, "Not a Telegram message id");
        }
        // Always send a markup: an empty one is how the buttons are taken away.
        body.set("reply_markup", inlineMarkup(inlineKeyboard == null ? List.of() : inlineKeyboard));
        call("editMessageText", body.toString(), Duration.ofSeconds(15));
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text) throws TelegramException {
        ObjectNode body = json.createObjectNode().put("callback_query_id", callbackQueryId);
        if (text != null && !text.isBlank()) {
            body.put("text", text.length() > 190 ? text.substring(0, 190) : text);
        }
        call("answerCallbackQuery", body.toString(), Duration.ofSeconds(10));
    }

    @Override
    public void setMyCommands(List<Command> commands, String languageCode, String chatId) throws TelegramException {
        ObjectNode body = json.createObjectNode();
        ArrayNode list = body.putArray("commands");
        commands.forEach(c -> list.addObject().put("command", c.command()).put("description", c.description()));
        if (chatId != null) {
            body.putObject("scope").put("type", "chat").put("chat_id", chatId);
        }
        if (languageCode != null) {
            body.put("language_code", languageCode);
        }
        call("setMyCommands", body.toString(), Duration.ofSeconds(10));
    }

    private ObjectNode inlineMarkup(List<List<Button>> keyboard) {
        ObjectNode markup = json.createObjectNode();
        ArrayNode rows = markup.putArray("inline_keyboard");
        for (List<Button> row : keyboard) {
            ArrayNode r = rows.addArray();
            row.forEach(b -> r.addObject().put("text", b.text()).put("callback_data", b.callbackData()));
        }
        return markup;
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
