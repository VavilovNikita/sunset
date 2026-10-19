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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> upgradeHeaders = new CopyOnWriteArrayList<>();
    /** While set, a getUpdates request is held until it is counted down (a call that never comes back). */
    private volatile CountDownLatch holdGetUpdates;
    private volatile int status = 200;
    private volatile String body = "{\"ok\":true,\"result\":{}}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            upgradeHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Upgrade")));
            CountDownLatch hold = holdGetUpdates;
            if (hold != null && exchange.getRequestURI().getPath().endsWith("/getUpdates")) {
                try {
                    hold.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        // One thread per request: the stand-in must be able to hold one request while it answers another.
        server.setExecutor(Executors.newCachedThreadPool());
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

    @Test
    void getUpdates_returnsAButtonPress_withTheMessageItSatUnder_andAsksForCallbackQueries() throws Exception {
        body = """
                {"ok":true,"result":[
                 {"update_id":20,"callback_query":{"id":"cq-1","data":"a:abc123","message":{"message_id":77,"chat":{"id":555,"type":"private"}}}},
                 {"update_id":21,"callback_query":{"id":"cq-2","message":{"message_id":78,"chat":{"id":555,"type":"private"}}}},
                 {"update_id":22,"callback_query":{"id":"cq-3","data":"x","message":{"message_id":79,"chat":{"id":-100,"type":"group"}}}}
                ]}""";

        List<TelegramUpdate> updates = client(TOKEN).getUpdates(20, 1);

        assertThat(updates.get(0).isCallback()).isTrue();
        assertThat(updates.get(0).chatId()).isEqualTo("555");
        assertThat(updates.get(0).callbackData()).isEqualTo("a:abc123");
        assertThat(updates.get(0).callbackQueryId()).isEqualTo("cq-1");
        assertThat(updates.get(0).messageId()).isEqualTo("77");
        // A press with no data, or from a group, is skipped but the offset still moves past it.
        assertThat(updates.subList(1, 3)).allSatisfy(u -> assertThat(u.chatId()).isNull());
        assertThat(bodies.get(0)).contains("\"callback_query\"").contains("\"message\"");
    }

    @Test
    void sendMessage_withButtons_postsAnInlineKeyboard_andReturnsTheMessageId() throws Exception {
        body = "{\"ok\":true,\"result\":{\"message_id\":4242}}";

        String id = client(TOKEN).sendMessage("555", "Approve?", List.of(List.of(new TelegramClient.Button("Approve", "a:xyz"))), null);

        assertThat(id).isEqualTo("4242");
        assertThat(bodies.get(0)).contains("inline_keyboard").contains("\"callback_data\":\"a:xyz\"").contains("\"text\":\"Approve\"");
    }

    @Test
    void sendMessage_withAMenu_postsAPersistentReplyKeyboard() throws Exception {
        client(TOKEN).sendMessage("555", "Menu", null, List.of(List.of("My shifts", "My day")));

        assertThat(bodies.get(0)).contains("\"keyboard\"").contains("My shifts").contains("is_persistent");
    }

    @Test
    void editing_answering_andSettingCommands_useTheirOwnMethods() throws Exception {
        HttpTelegramClient client = client(TOKEN);

        client.editMessageText("555", "77", "Approved by Hana", null);
        client.answerCallbackQuery("cq-1", "Approved.");
        client.setMyCommands(List.of(new TelegramClient.Command("shifts", "My shifts")), "ru", "555");

        assertThat(paths).containsExactly("/bot" + TOKEN + "/editMessageText", "/bot" + TOKEN + "/answerCallbackQuery", "/bot" + TOKEN + "/setMyCommands");
        // Editing without buttons sends an empty keyboard: that is how Telegram removes the old ones.
        assertThat(bodies.get(0)).contains("\"message_id\":77").contains("\"inline_keyboard\":[]");
        assertThat(bodies.get(2)).contains("\"language_code\":\"ru\"").contains("\"chat_id\":\"555\"");
    }

    @Test
    void aMessageIdThatIsNotANumber_isRefusedBeforeAnyRequest() {
        assertThatThrownBy(() -> client(TOKEN).editMessageText("555", "not-a-number", "x", null)).isInstanceOf(TelegramException.class);
        assertThat(paths).isEmpty();
    }

    @Test
    void itSpeaksHttp11_withoutAskingToUpgradeToHttp2() throws Exception {
        client(TOKEN).sendMessage("555", "hello");

        // The JDK client's default would offer "Upgrade: h2c" on a plain connection (and negotiate h2 over TLS).
        assertThat(upgradeHeaders).containsExactly("null");
    }

    @Test
    void aCallThatNeverComesBack_doesNotHoldUpTheOthers() throws Exception {
        holdGetUpdates = new CountDownLatch(1);
        HttpTelegramClient client = client(TOKEN);
        Thread longPoll = new Thread(() -> {
            try {
                client.getUpdates(0, 1);
            } catch (TelegramException e) {
                // released at the end of the test
            }
        });
        longPoll.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!paths.stream().anyMatch(p -> p.endsWith("/getUpdates")) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(paths).anyMatch(p -> p.endsWith("/getUpdates"));

        long started = System.nanoTime();
        client.answerCallbackQuery("cq-1", null);
        client.sendMessage("555", "still works");
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        holdGetUpdates.countDown();
        longPoll.join(15_000);
        assertThat(tookMs).as("both calls finished while the long poll was still held").isLessThan(3_000);
    }
}
