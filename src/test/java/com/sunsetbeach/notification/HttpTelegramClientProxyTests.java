package com.sunsetbeach.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import com.sunsetbeach.notification.TelegramClient.Route;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.support.MutableClock;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * The optional proxy and the way back to a direct connection, against two local stand-ins: one plays Telegram, the
 * other the hotel's proxy (for a plain-http target a JDK client sends the request to the proxy itself, so the stand-in
 * sees it and Telegram does not). Never the real bot.
 */
class HttpTelegramClientProxyTests {

    private static final String TOKEN = "123456:SECRET-TOKEN-VALUE";
    private static final String OK = "{\"ok\":true,\"result\":[]}";

    private HttpServer telegramServer;
    private HttpServer proxyServer;
    private final List<String> telegramHits = new CopyOnWriteArrayList<>();
    private final List<String> proxyHits = new CopyOnWriteArrayList<>();
    /** While set, the proxy takes the connection and drops it without an answer (a dead tunnel). */
    private volatile boolean proxyDown;
    private volatile int proxyStatus = 200;
    private volatile String proxyBody = OK;
    private final MutableClock clock = new MutableClock("2026-10-20T10:00");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final List<Logger> loggers = List.of(
            (Logger) LoggerFactory.getLogger(HttpTelegramClient.class), (Logger) LoggerFactory.getLogger(TelegramRouteSelector.class));

    @BeforeEach
    void start() throws IOException {
        telegramServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        telegramServer.createContext("/", exchange -> {
            telegramHits.add(exchange.getRequestURI().getPath());
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = OK.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        telegramServer.setExecutor(Executors.newCachedThreadPool());
        telegramServer.start();

        proxyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxyServer.createContext("/", exchange -> {
            proxyHits.add(exchange.getRequestURI().toString());
            exchange.getRequestBody().readAllBytes();
            if (proxyDown) {
                exchange.close();
                return;
            }
            byte[] bytes = proxyBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(proxyStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        proxyServer.setExecutor(Executors.newCachedThreadPool());
        proxyServer.start();

        logs.start();
        loggers.forEach(l -> l.addAppender(logs));
    }

    @AfterEach
    void stop() {
        loggers.forEach(l -> l.detachAppender(logs));
        telegramServer.stop(0);
        proxyServer.stop(0);
    }

    private HttpTelegramClient client(String proxy) {
        return new HttpTelegramClient(TOKEN, "http://127.0.0.1:" + telegramServer.getAddress().getPort(), proxy, 3, 300, clock, JsonMapper.builder().build());
    }

    private String proxyAddress() {
        return "127.0.0.1:" + proxyServer.getAddress().getPort();
    }

    private List<String> warnsAndInfos() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void withoutAProxy_everythingGoesDirect_asBefore() throws Exception {
        HttpTelegramClient client = client("");

        client.getUpdates(0, 1);
        client.sendMessage("1", "x");

        assertThat(client.proxyConfigured()).isFalse();
        assertThat(client.route()).isEqualTo(Route.DIRECT);
        assertThat(telegramHits).hasSize(2);
        assertThat(proxyHits).isEmpty();
        assertThat(warnsAndInfos()).isEmpty();
    }

    @Test
    void withAProxy_pollsAndSendsGoThroughIt_andTelegramIsNeverCalledDirectly() throws Exception {
        HttpTelegramClient client = client(proxyAddress());

        client.getUpdates(0, 1);
        client.sendMessage("1", "x");
        client.answerCallbackQuery("cq", null);

        assertThat(client.proxyConfigured()).isTrue();
        assertThat(client.route()).isEqualTo(Route.PROXY);
        assertThat(proxyHits).hasSize(3).allSatisfy(uri -> assertThat(uri).startsWith("http://127.0.0.1:" + telegramServer.getAddress().getPort() + "/bot" + TOKEN + "/"));
        assertThat(telegramHits).isEmpty();
        // The address is fine to log, once, at startup; the token is not fine anywhere.
        assertThat(warnsAndInfos()).hasSize(1).first().asString().contains(proxyAddress()).doesNotContain(TOKEN);
    }

    @Test
    void aProxyThatStopsAnswering_isLeftAfterTheThreshold_andComesBackWhenItAnswers() throws Exception {
        HttpTelegramClient client = client(proxyAddress());
        client.getUpdates(0, 1);
        proxyDown = true;

        // Two failures in a row are not yet "failing": still the proxy.
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> client.getUpdates(0, 1)).isInstanceOf(TelegramException.class);
            assertThat(client.route()).isEqualTo(Route.PROXY);
        }
        // The third one switches; that poll still fails, the next one goes straight to Telegram.
        assertThatThrownBy(() -> client.getUpdates(0, 1)).isInstanceOf(TelegramException.class);
        assertThat(client.route()).isEqualTo(Route.DIRECT);
        assertThat(client.proxyConfigured()).isTrue();
        assertThat(telegramHits).isEmpty();

        client.getUpdates(0, 1);
        client.sendMessage("1", "x");
        assertThat(telegramHits).hasSize(2);

        // Not yet time to look at the proxy: it is not called at all.
        int proxyCalls = proxyHits.size();
        clock.set("2026-10-20T10:04");
        client.getUpdates(0, 1);
        assertThat(proxyHits).hasSize(proxyCalls);

        // Time, but it is still down: the probe fails quietly, the poll is answered over the fallback.
        clock.set("2026-10-20T10:06");
        client.getUpdates(0, 1);
        assertThat(proxyHits).hasSize(proxyCalls + 1);
        assertThat(client.route()).isEqualTo(Route.DIRECT);
        assertThat(telegramHits).hasSize(4);
        // ...and the next look is a full interval later, not at the next poll.
        client.getUpdates(0, 1);
        assertThat(proxyHits).hasSize(proxyCalls + 1);

        // It answers again.
        proxyDown = false;
        clock.set("2026-10-20T10:12");
        client.getUpdates(0, 1);
        assertThat(client.route()).isEqualTo(Route.PROXY);
        client.sendMessage("1", "x");
        assertThat(proxyHits.get(proxyHits.size() - 1)).endsWith("/sendMessage");
        assertThat(telegramHits).hasSize(5);

        assertThat(warnsAndInfos()).filteredOn(m -> !m.startsWith("Telegram requests go")).containsExactly(
                "Telegram via proxy failing, switching to direct",
                "Telegram via proxy answers again, switching back to proxy");
        assertThat(warnsAndInfos()).noneMatch(m -> m.contains(TOKEN));
    }

    @Test
    void anAnswerFromTelegram_evenARefusal_provesThePathAndResetsTheCount() throws Exception {
        HttpTelegramClient client = client(proxyAddress());
        for (int round = 0; round < 3; round++) {
            proxyDown = true;
            for (int i = 0; i < 2; i++) {
                assertThatThrownBy(() -> client.getUpdates(0, 1)).isInstanceOf(TelegramException.class);
            }
            proxyDown = false;
            proxyStatus = 409;
            proxyBody = "{\"ok\":false,\"error_code\":409,\"description\":\"Conflict\"}";
            assertThatThrownBy(() -> client.getUpdates(0, 1)).isInstanceOf(TelegramException.class);
            proxyStatus = 200;
            proxyBody = OK;
        }

        assertThat(client.route()).isEqualTo(Route.PROXY);
    }

    @Test
    void aSendThatFails_doesNotMoveTheRoute() {
        HttpTelegramClient client = client(proxyAddress());
        proxyDown = true;

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.sendMessage("1", "x")).isInstanceOf(TelegramException.class);
        }

        assertThat(client.route()).isEqualTo(Route.PROXY);
    }

    @Test
    void aTunnelThatTheProxyRefuses_isAFailureWithoutTheUrlOrTheTokenInIt() throws Exception {
        // What tinyproxy does with anything but CONNECT to Telegram: a 403 to the CONNECT itself.
        AtomicReference<String> requestLine = new AtomicReference<>();
        try (ServerSocket denier = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))) {
            Thread t = Thread.ofVirtual().start(() -> {
                try (Socket s = denier.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                    requestLine.set(in.readLine());
                    s.getOutputStream().write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    s.getOutputStream().flush();
                } catch (IOException e) {
                    // the client gave up first; the assertions below say what matters
                }
            });
            HttpTelegramClient client = new HttpTelegramClient(TOKEN, "https://api.telegram.org", "127.0.0.1:" + denier.getLocalPort(), 3, 300, clock, JsonMapper.builder().build());

            assertThatThrownBy(() -> client.getBotUsername()).isInstanceOfSatisfying(TelegramException.class, e -> {
                assertThat(e.kind()).isEqualTo(TelegramException.Kind.UNAVAILABLE);
                assertThat(e.getMessage()).doesNotContain("api.telegram.org").doesNotContain(TOKEN);
            });
            t.join(5_000);
        }
        assertThat(requestLine.get()).isEqualTo("CONNECT api.telegram.org:443 HTTP/1.1");
    }

    @Test
    void aMalformedProxy_stopsStartWithAMessageThatSaysWhatIsExpected() {
        for (String bad : new String[] {"http://10.8.0.2:8888", "10.8.0.2", "10.8.0.2:", ":8888", "10.8.0.2:abc", "10.8.0.2:0", "10.8.0.2:70000", "10.8.0.2:8888/", "a:b:8888", "user@10.8.0.2:8888"}) {
            assertThatThrownBy(() -> client(bad)).as(bad).isInstanceOf(IllegalStateException.class).hasMessageStartingWith("TELEGRAM_PROXY must be host:port");
        }
    }

    @Test
    void blankAndBracketedIpv6Values_areUnderstood() {
        assertThat(HttpTelegramClient.parseProxy("  ")).isNull();
        assertThat(HttpTelegramClient.parseProxy(null)).isNull();
        assertThat(HttpTelegramClient.parseProxy(" 10.8.0.2:8888 ").getHostString()).isEqualTo("10.8.0.2");
        InetSocketAddress v6 = HttpTelegramClient.parseProxy("[fd00::2]:3128");
        assertThat(v6.getHostString()).isEqualTo("fd00::2");
        assertThat(v6.getPort()).isEqualTo(3128);
    }
}
