package com.sunsetbeach.lighting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * WLED's JSON API over {@code java.net.http}. Against a hostile or confused target the rules are:
 * the address is resolved and checked by {@link LanAddressGuard} for every request and the request
 * goes to that <em>IP</em> (a changing DNS answer cannot redirect it, and an IP in the {@code Host}
 * header also keeps WLED's captive-portal 302 out of the picture); redirects are never followed (a
 * 3xx is a failure); the whole exchange has a hard deadline and the body is capped, so a device that
 * trickles bytes cannot hold a thread; and what the device sends is parsed, never copied into a
 * message - errors say what happened in our own words and the details go to the log.
 */
@Component
public class HttpWledClient implements WledClient {

    private static final Logger log = LoggerFactory.getLogger(HttpWledClient.class);

    /** The biggest answer read (presets.json of a full controller is far below this). */
    private static final int MAX_BODY_BYTES = 1 << 20;

    private final HttpClient http;
    private final long requestTimeoutMillis;
    private final Function<String, InetAddress> addressFor;
    private final JsonMapper json;

    @Autowired
    public HttpWledClient(
            @Value("${app.lighting.connect-timeout-ms:2000}") long connectTimeoutMillis,
            @Value("${app.lighting.request-timeout-ms:2500}") long requestTimeoutMillis,
            LanAddressGuard guard,
            JsonMapper json) {
        this(connectTimeoutMillis, requestTimeoutMillis, host -> {
            try {
                return guard.resolve(host);
            } catch (LanAddressGuard.RefusedHostException e) {
                throw new WledException(WledException.Kind.REFUSED_ADDRESS, "has an address that is not an allowed private LAN address (" + e.getMessage() + ")");
            }
        }, json);
    }

    /** Test seam: how a host becomes an address. Production always goes through the guard. */
    HttpWledClient(long connectTimeoutMillis, long requestTimeoutMillis, Function<String, InetAddress> addressFor, JsonMapper json) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.addressFor = addressFor;
        this.json = json;
    }

    @Override
    public WledInfo info(String host, int port) {
        JsonNode root = getJson(host, port, "/json/info");
        JsonNode version = root.path("ver");
        JsonNode count = root.path("leds").path("count");
        if (!version.isString() || !count.isNumber()) {
            log.warn("{}:{} answered /json/info without ver/leds.count - not WLED", host, port);
            throw new WledException(WledException.Kind.BAD_RESPONSE, "answered, but it does not look like a WLED controller");
        }
        return new WledInfo(root.path("name").asString(""), version.asString(), count.asInt(), root.path("mac").asString(""));
    }

    @Override
    public WledState state(String host, int port) {
        JsonNode root = getJson(host, port, "/json/state");
        if (!root.path("on").isBoolean() || !root.path("bri").isNumber()) {
            log.warn("{}:{} answered /json/state without on/bri", host, port);
            throw new WledException(WledException.Kind.BAD_RESPONSE, "sent a state that is not WLED's");
        }
        int ps = root.path("ps").asInt(-1);
        return new WledState(root.path("on").asBoolean(), root.path("bri").asInt(), ps > 0 ? ps : null);
    }

    /**
     * {@code /presets.json} is an object keyed by preset id whose values carry the name in {@code n}.
     * Anything else in it is skipped: id 0 (WLED keeps an empty placeholder there), entries that are not
     * objects, keys that are not a number from 1 to 250. A preset without a name is "Preset N".
     */
    @Override
    public List<WledPreset> presets(String host, int port) {
        JsonNode root = getJson(host, port, "/presets.json");
        List<WledPreset> result = new ArrayList<>();
        if (root.isObject()) {
            for (Map.Entry<String, JsonNode> entry : root.properties()) {
                int id;
                try {
                    id = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (id < 1 || id > 250 || !entry.getValue().isObject()) {
                    continue;
                }
                String name = entry.getValue().path("n").asString("").strip();
                result.add(new WledPreset(id, name.isEmpty() ? "Preset " + id : name));
            }
        }
        result.sort(Comparator.comparingInt(WledPreset::id));
        return result;
    }

    @Override
    public void setState(String host, int port, WledStateChange change) {
        StringBuilder body = new StringBuilder("{");
        if (change.on() != null) {
            body.append("\"on\":").append(change.on()).append(',');
        }
        if (change.brightness() != null) {
            body.append("\"bri\":").append(change.brightness().intValue()).append(',');
        }
        if (change.presetId() != null) {
            body.append("\"ps\":").append(change.presetId().intValue()).append(',');
        }
        if (body.length() == 1) {
            throw new IllegalArgumentException("nothing to send");
        }
        body.setLength(body.length() - 1);
        body.append('}');
        HttpRequest request = request(host, port, "/json/state")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        send(request, host, port); // WLED answers {"success":true}; only the status matters
    }

    private JsonNode getJson(String host, int port, String path) {
        byte[] body = send(request(host, port, path).GET().build(), host, port);
        try {
            return json.readTree(body);
        } catch (RuntimeException e) {
            log.warn("{}:{}{} answered something that is not JSON", host, port, path);
            throw new WledException(WledException.Kind.BAD_RESPONSE, "sent an answer that is not WLED's");
        }
    }

    private HttpRequest.Builder request(String host, int port, String path) {
        InetAddress address = addressFor.apply(host);
        URI uri = URI.create("http://" + address.getHostAddress() + ":" + port + path);
        return HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(requestTimeoutMillis)).header("Accept", "application/json");
    }

    /** One exchange with a hard overall deadline; returns the (capped) body of a 2xx answer. */
    private byte[] send(HttpRequest request, String host, int port) {
        CompletableFuture<HttpResponse<byte[]>> future = http.sendAsync(
                request, info -> new CappedBody(MAX_BODY_BYTES));
        HttpResponse<byte[]> response;
        try {
            response = future.get(requestTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("{}:{} did not answer within {} ms", host, port, requestTimeoutMillis);
            throw new WledException(WledException.Kind.UNREACHABLE, "did not answer in time");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new WledException(WledException.Kind.UNREACHABLE, "did not answer");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.warn("{}:{} could not be reached: {}", host, port, cause == null ? e : cause.getClass().getSimpleName());
            if (cause instanceof WledException wled) {
                throw wled;
            }
            throw new WledException(WledException.Kind.UNREACHABLE, cause instanceof IOException ? "could not be reached" : "did not answer");
        }
        int status = response.statusCode();
        if (status >= 300 && status < 400) {
            log.warn("{}:{} answered with a redirect ({}); not followed", host, port, status);
            throw new WledException(WledException.Kind.BAD_RESPONSE, "tried to redirect the request");
        }
        if (status < 200 || status >= 300) {
            log.warn("{}:{} answered HTTP {}", host, port, status);
            throw new WledException(WledException.Kind.BAD_RESPONSE, "answered with an error (HTTP " + status + ")");
        }
        return response.body();
    }

    /**
     * Collects the body but stops reading (cancels the stream) once {@code cap} bytes have arrived; what
     * was kept is then cut short and fails to parse, which is the right outcome for an oversized answer.
     * ({@code BodySubscribers.limiting} only exists from JDK 22.)
     */
    private static final class CappedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int cap;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private boolean done;

        CappedBody(int cap) {
            this.cap = cap;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (done) {
                return;
            }
            for (ByteBuffer item : items) {
                int take = Math.min(item.remaining(), cap - buffer.size());
                byte[] chunk = new byte[take];
                item.get(chunk);
                buffer.write(chunk, 0, take);
            }
            if (buffer.size() >= cap) {
                done = true;
                subscription.cancel();
                result.complete(buffer.toByteArray());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            done = true;
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                result.complete(buffer.toByteArray());
            }
        }
    }
}
