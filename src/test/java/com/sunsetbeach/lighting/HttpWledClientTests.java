package com.sunsetbeach.lighting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sunsetbeach.lighting.WledClient.WledInfo;
import com.sunsetbeach.lighting.WledClient.WledPreset;
import com.sunsetbeach.lighting.WledClient.WledState;
import com.sunsetbeach.lighting.WledClient.WledStateChange;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link HttpWledClient} against a throw-away HTTP server on the loopback interface (the test seam
 * skips the address guard, which would refuse loopback - the production wiring is covered by the last
 * test): the parsing of WLED's answers, the request that is sent, and the three things that matter
 * against a hostile or broken device - redirects are not followed, a device's body is never put into
 * an error, and a device that stalls or floods is cut off.
 */
class HttpWledClientTests {

    private static final String INFO = "{\"ver\":\"0.15.0\",\"vid\":2405180,\"leds\":{\"count\":150},\"name\":\"Pool bar\",\"mac\":\"60019423b441\"}";
    private static final String STATE = "{\"on\":true,\"bri\":128,\"transition\":7,\"ps\":3,\"seg\":[{\"id\":0}]}";

    private HttpServer server;
    private HttpWledClient client;
    private final Map<String, Handler> routes = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();
    private final AtomicReference<String> lastHostHeader = new AtomicReference<>();

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws Exception;
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try {
                lastHostHeader.set(exchange.getRequestHeaders().getFirst("Host"));
                lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                Handler handler = routes.get(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
                if (handler == null) {
                    reply(exchange, 404, "no route");
                } else {
                    handler.handle(exchange);
                }
            } catch (Exception e) {
                exchange.close();
            }
        });
        server.start();
        client = clientWithTimeouts(1000, 1000);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpWledClient clientWithTimeouts(long connectMs, long requestMs) {
        return new HttpWledClient(connectMs, requestMs, host -> InetAddress.getLoopbackAddress(), JsonMapper.builder().build());
    }

    private int port() {
        return server.getAddress().getPort();
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    // ---- reading -------------------------------------------------------------------------------

    @Test
    void info_isReadFromJsonInfo() {
        routes.put("GET /json/info", e -> reply(e, 200, INFO));

        WledInfo info = client.info("controller", port());

        assertThat(info).isEqualTo(new WledInfo("Pool bar", "0.15.0", 150, "60019423b441"));
    }

    @Test
    void somethingThatIsNotWled_isRejected_andNeverEchoed() {
        routes.put("GET /json/info", e -> reply(e, 200, "{\"hello\":\"I am a router admin page SECRET-MARKER\"}"));

        assertThatThrownBy(() -> client.info("controller", port()))
                .isInstanceOfSatisfying(WledException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(WledException.Kind.BAD_RESPONSE);
                    assertThat(e.getMessage()).contains("does not look like a WLED").doesNotContain("SECRET-MARKER").doesNotContain("router");
                });
    }

    @Test
    void state_readsOnBriAndPreset_andMinusOneMeansNoPreset() {
        routes.put("GET /json/state", e -> reply(e, 200, STATE));
        assertThat(client.state("c", port())).isEqualTo(new WledState(true, 128, 3));

        routes.put("GET /json/state", e -> reply(e, 200, "{\"on\":false,\"bri\":40,\"ps\":-1}"));
        assertThat(client.state("c", port())).isEqualTo(new WledState(false, 40, null));
    }

    @Test
    void presets_areReadFromPresetsJson_andOddEntriesAreSkipped() {
        routes.put("GET /presets.json", e -> reply(e, 200, """
                {"0":{},"2":{"n":"Sunset","on":true,"bri":200},"1":{"n":"  Warm white "},"5":{"playlist":{"ps":[1,2]}},
                 "7":"not an object","abc":{"n":"nope"},"251":{"n":"too high"},"-3":{"n":"negative"}}"""));

        List<WledPreset> presets = client.presets("c", port());

        assertThat(presets).containsExactly(new WledPreset(1, "Warm white"), new WledPreset(2, "Sunset"), new WledPreset(5, "Preset 5"));
    }

    // ---- commanding ----------------------------------------------------------------------------

    @Test
    void setState_postsOnlyTheFieldsThatAreSet() {
        routes.put("POST /json/state", e -> reply(e, 200, "{\"success\":true}"));

        client.setState("c", port(), new WledStateChange(true, 200, null));
        assertThat(lastBody.get()).isEqualTo("{\"on\":true,\"bri\":200}");
        assertThat(lastContentType.get()).startsWith("application/json");

        client.setState("c", port(), new WledStateChange(null, null, 4));
        assertThat(lastBody.get()).isEqualTo("{\"ps\":4}");

        client.setState("c", port(), new WledStateChange(false, null, null));
        assertThat(lastBody.get()).isEqualTo("{\"on\":false}");
    }

    @Test
    void requestsGoToTheResolvedIp_notToTheNameTheAdminTyped() {
        routes.put("GET /json/info", e -> reply(e, 200, INFO));

        client.info("some-name-that-resolves-elsewhere.example", port());

        assertThat(lastHostHeader.get()).startsWith("127.0.0.1:");
    }

    // ---- hostile or broken devices -------------------------------------------------------------

    @Test
    void aRedirect_isNeverFollowed() {
        AtomicInteger targetHits = new AtomicInteger();
        routes.put("GET /json/info", e -> {
            e.getResponseHeaders().add("Location", "http://127.0.0.1:" + port() + "/internal-admin");
            e.sendResponseHeaders(302, -1);
            e.close();
        });
        routes.put("GET /internal-admin", e -> {
            targetHits.incrementAndGet();
            reply(e, 200, INFO);
        });

        assertThatThrownBy(() -> client.info("c", port()))
                .isInstanceOfSatisfying(WledException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(WledException.Kind.BAD_RESPONSE);
                    assertThat(e.getMessage()).contains("redirect").doesNotContain("internal-admin");
                });
        assertThat(targetHits).hasValue(0);
    }

    @Test
    void aRedirectOnACommand_isNeverFollowedEither() {
        AtomicInteger targetHits = new AtomicInteger();
        routes.put("POST /json/state", e -> {
            e.getResponseHeaders().add("Location", "/elsewhere");
            e.sendResponseHeaders(307, -1);
            e.close();
        });
        routes.put("POST /elsewhere", e -> {
            targetHits.incrementAndGet();
            reply(e, 200, "{}");
        });

        assertThatThrownBy(() -> client.setState("c", port(), new WledStateChange(true, null, null))).isInstanceOf(WledException.class);
        assertThat(targetHits).hasValue(0);
    }

    @Test
    void anErrorStatus_isReportedByNumber_andTheBodyNeverAppearsInTheMessage() {
        routes.put("GET /json/state", e -> reply(e, 500, "{\"error\":\"stack trace SECRET-MARKER at /etc/passwd\"}"));

        assertThatThrownBy(() -> client.state("c", port()))
                .isInstanceOfSatisfying(WledException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(WledException.Kind.BAD_RESPONSE);
                    assertThat(e.getMessage()).contains("HTTP 500").doesNotContain("SECRET-MARKER").doesNotContain("passwd");
                });
    }

    @Test
    void aBodyThatIsNotJson_isNeverEchoed() {
        routes.put("GET /json/state", e -> reply(e, 200, "<html>SECRET-MARKER</html>"));

        assertThatThrownBy(() -> client.state("c", port()))
                .isInstanceOfSatisfying(WledException.class, e -> assertThat(e.getMessage()).doesNotContain("SECRET-MARKER").doesNotContain("html"));
    }

    @Test
    void aDeviceThatStalls_isCutOffAtTheDeadline() {
        HttpWledClient quick = clientWithTimeouts(500, 400);
        routes.put("GET /json/state", e -> {
            Thread.sleep(3000);
            reply(e, 200, STATE);
        });

        long started = System.nanoTime();
        assertThatThrownBy(() -> quick.state("c", port()))
                .isInstanceOfSatisfying(WledException.class, e -> assertThat(e.getKind()).isEqualTo(WledException.Kind.UNREACHABLE));
        assertThat((System.nanoTime() - started) / 1_000_000).as("ms until it gave up").isLessThan(2000);
    }

    @Test
    void aDeviceThatTricklesTheBody_isCutOffAtTheDeadlineToo() {
        HttpWledClient quick = clientWithTimeouts(500, 600);
        routes.put("GET /json/state", e -> {
            e.sendResponseHeaders(200, 0);
            for (int i = 0; i < 30; i++) {
                e.getResponseBody().write(' ');
                e.getResponseBody().flush();
                Thread.sleep(200);
            }
            e.close();
        });

        long started = System.nanoTime();
        assertThatThrownBy(() -> quick.state("c", port())).isInstanceOf(WledException.class);
        assertThat((System.nanoTime() - started) / 1_000_000).isLessThan(2500);
    }

    @Test
    void aNothingListeningPort_isUnreachable() throws Exception {
        int dead;
        try (java.net.ServerSocket probe = new java.net.ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            dead = probe.getLocalPort();
        }

        int closedPort = dead;
        assertThatThrownBy(() -> client.info("c", closedPort))
                .isInstanceOfSatisfying(WledException.class, e -> assertThat(e.getKind()).isEqualTo(WledException.Kind.UNREACHABLE));
    }

    @Test
    void anOversizedAnswer_isCutAndRejected_insteadOfBeingReadWhole() {
        char[] padding = new char[2 * 1024 * 1024];
        Arrays.fill(padding, 'x');
        // valid JSON when read whole - so a failure here can only come from the cap
        routes.put("GET /presets.json", e -> reply(e, 200, "{\"1\":{\"n\":\"a\"},\"pad\":\"" + new String(padding) + "\"}"));

        assertThatThrownBy(() -> client.presets("c", port())).isInstanceOf(WledException.class);
    }

    // ---- production wiring ---------------------------------------------------------------------

    @Test
    void theProductionClient_refusesANonLanAddress_beforeAnyConnection() {
        LanAddressGuard realGuard = new LanAddressGuard("10.0.0.0/8,192.168.0.0/16", InetAddress::getAllByName, a -> false);
        HttpWledClient production = new HttpWledClient(500, 500, realGuard, JsonMapper.builder().build());

        for (String host : List.of("127.0.0.1", "169.254.169.254", "8.8.8.8")) {
            assertThatThrownBy(() -> production.state(host, port()))
                    .as(host)
                    .isInstanceOfSatisfying(WledException.class, e -> assertThat(e.getKind()).isEqualTo(WledException.Kind.REFUSED_ADDRESS));
        }
        assertThatThrownBy(() -> production.setState("127.0.0.1", port(), new WledStateChange(true, null, null))).isInstanceOf(WledException.class);
        assertThat(lastHostHeader.get()).as("the loopback server was never contacted").isNull();
    }
}
