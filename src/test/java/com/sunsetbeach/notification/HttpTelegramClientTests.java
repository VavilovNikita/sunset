package com.sunsetbeach.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Bot API client against a local stand-in. What matters is what it must never do: put the bot
 * token (which is in every URL) into an exception or an answer, or copy Telegram's raw reply into a
 * message.
 */
class HttpTelegramClientTests {

    private static final String TOKEN = "123456:SECRET-TOKEN-VALUE";

    private HttpServer server;
    private final List<String> paths = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = "{\"ok\":true,\"result\":{}}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpTelegramClient client(String token) {
        return new HttpTelegramClient(token, "http://127.0.0.1:" + server.getAddress().getPort(), JsonMapper.builder().build());
    }

    @Test
    void noToken_meansNotConfigured_andNothingIsCalled() {
        HttpTelegramClient client = client("  ");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.sendMessage("1", "x")).isInstanceOf(TelegramException.class);
        assertThat(paths).isEmpty();
    }

    @Test
    void getMe_readsTheBotsUsername() throws Exception {
        body = "{\"ok\":true,\"result\":{\"id\":1,\"is_bot\":true,\"username\":\"sunset_bot\"}}";

        assertThat(client(TOKEN).getBotUsername()).isEqualTo("sunset_bot");
        assertThat(paths).containsExactly("/bot" + TOKEN + "/getMe");
    }

    @Test
    void getUpdates_keepsPrivateTextMessages_andSkipsEverythingElseButStillAdvances() throws Exception {
        body = """
                {"ok":true,"result":[
                 {"update_id":10,"message":{"chat":{"id":555,"type":"private"},"text":"/start abc"}},
                 {"update_id":11,"message":{"chat":{"id":-100,"type":"group"},"text":"hello"}},
                 {"update_id":12,"edited_message":{"chat":{"id":555,"type":"private"},"text":"x"}},
                 {"update_id":13,"message":{"chat":{"id":555,"type":"private"},"sticker":{}}}
                ]}""";

        List<TelegramUpdate> updates = client(TOKEN).getUpdates(10, 1);

        assertThat(updates).extracting(TelegramUpdate::updateId).containsExactly(10L, 11L, 12L, 13L);
        assertThat(updates.get(0).chatId()).isEqualTo("555");
        assertThat(updates.get(0).text()).isEqualTo("/start abc");
        assertThat(updates.subList(1, 4)).allSatisfy(u -> assertThat(u.text()).isNull());
    }

    @Test
    void aBlockedBot_isReportedAsBlocked_withoutTheRawAnswerOrTheToken() {
        status = 403;
        body = "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}";

        assertThatThrownBy(() -> client(TOKEN).sendMessage("555", "hi"))
                .isInstanceOfSatisfying(TelegramException.class, e -> {
                    assertThat(e.kind()).isEqualTo(TelegramException.Kind.BLOCKED);
                    assertThat(e.getMessage()).doesNotContain("Forbidden").doesNotContain(TOKEN).doesNotContain("SECRET");
                });
    }

    @Test
    void rateLimitsAndOutages_areWorthRetrying_otherRefusalsAreNot() {
        status = 429;
        body = "{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests: retry after 5\"}";
        assertThatThrownBy(() -> client(TOKEN).sendMessage("1", "x"))
                .isInstanceOfSatisfying(TelegramException.class, e -> assertThat(e.kind()).isEqualTo(TelegramException.Kind.UNAVAILABLE));

        status = 502;
        body = "<html>Bad gateway</html>";
        assertThatThrownBy(() -> client(TOKEN).sendMessage("1", "x"))
                .isInstanceOfSatisfying(TelegramException.class, e -> {
                    assertThat(e.kind()).isEqualTo(TelegramException.Kind.UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain("Bad gateway");
                });

        status = 400;
        body = "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}";
        assertThatThrownBy(() -> client(TOKEN).sendMessage("1", "x"))
                .isInstanceOfSatisfying(TelegramException.class, e -> {
                    assertThat(e.kind()).isEqualTo(TelegramException.Kind.REJECTED);
                    assertThat(e.getMessage()).doesNotContain("chat not found");
                });
    }

    @Test
    void anUnreachableTelegram_isUnavailable_andTheMessageNamesNeitherUrlNorToken() {
        int port = server.getAddress().getPort();
        server.stop(0);
        HttpTelegramClient client = new HttpTelegramClient(TOKEN, "http://127.0.0.1:" + port, JsonMapper.builder().build());

        assertThatThrownBy(() -> client.sendMessage("1", "x"))
                .isInstanceOfSatisfying(TelegramException.class, e -> {
                    assertThat(e.kind()).isEqualTo(TelegramException.Kind.UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain("127.0.0.1").doesNotContain(TOKEN).doesNotContain("bot");
                });
    }

    @Test
    void sendMessage_postsToSendMessage() throws Exception {
        client(TOKEN).sendMessage("555", "hello");

        assertThat(paths).containsExactly("/bot" + TOKEN + "/sendMessage");
    }
}
