package com.sunsetbeach.tuya;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Tuya cloud API: request signing (HMAC-SHA256, "new" signature with an access token), a token
 * cached until shortly before it expires, timeouts, and Tuya's failures turned into {@link
 * TuyaException}s with a message for the person at the screen. Every call it makes is counted
 * ({@link TuyaUsageCounter}) because the Trial Edition quota is finite.
 *
 * <p>Nothing here polls. Callers read state when a page opens or after a command, and that is the
 * only thing that spends quota. The key, the secret and the access token are never logged. Tuya's
 * device list also carries each device's {@code localKey}; this class never copies it out of the
 * response.
 *
 * <p>What was observed against the real project (2026-10-13) and what was not: the token, device
 * list ({@code /v2.0/cloud/thing/device}), device info ({@code /v1.0/devices/{id}}, which carries
 * {@code online} and {@code status}) and the "API is not subscribed" answer (28841101) were seen.
 * Commands, the IR scene command and the remotes list were not (the IR APIs are not subscribed on
 * the project yet); the codes {@link #TOKEN_EXPIRED} and {@link #SUBSCRIPTION_EXPIRED} come from
 * Tuya's public error table, not from this project.
 */
@Component
public class TuyaClient {

    private static final Logger log = LoggerFactory.getLogger(TuyaClient.class);

    static final String MSG_UNAVAILABLE =
            "Интеграция Tuya недоступна — проверьте подписку IoT Core (продление пробного периода на iot.tuya.com)";
    static final String MSG_NOT_CONFIGURED =
            "Интеграция Tuya не настроена — на сервере не заданы TUYA_CLIENT_ID, TUYA_CLIENT_SECRET и TUYA_API_BASE";
    static final String MSG_API_NOT_SUBSCRIBED =
            "Этот API не подключён к проекту Tuya — подключите нужный сервис в проекте на iot.tuya.com (Cloud → Development → Service API)";
    static final String MSG_REJECTED = "Tuya отклонила запрос. Подробности — в журнале сервера";
    static final String MSG_NETWORK = "Tuya не отвечает — попробуйте ещё раз через минуту";

    /** Seen on this project: the called API is not part of the project's subscriptions. */
    static final long API_NOT_SUBSCRIBED = 28841101L;
    /** Tuya's table: the access token is invalid or expired. Handled by fetching a fresh one once. */
    static final long TOKEN_EXPIRED = 1010L;
    /** Tuya's table: the cloud development plan subscription has expired. */
    static final long SUBSCRIPTION_EXPIRED = 28841002L;

    private static final long TOKEN_EARLY_REFRESH_MS = 60_000L;
    private static final int DEVICE_PAGE_SIZE = 20;
    private static final int DEVICE_MAX_PAGES = 10;

    private final String clientId;
    private final String clientSecret;
    private final boolean configured;
    private final Clock clock;
    private final TuyaUsageCounter usage;
    private final JsonMapper json;
    private final RestClient rest;

    private final Object tokenLock = new Object();
    private volatile String accessToken;
    private volatile long accessTokenExpiresAtMs;

    @Autowired
    public TuyaClient(
            @Value("${app.tuya.client-id:}") String clientId,
            @Value("${app.tuya.client-secret:}") String clientSecret,
            @Value("${app.tuya.api-base:}") String apiBase,
            Clock clock,
            TuyaUsageCounter usage,
            JsonMapper json) {
        this(clientId, clientSecret, apiBase, clock, usage, json, buildRestClient(apiBase));
    }

    /** For tests: a RestClient already pointed at a mock server. */
    public TuyaClient(
            String clientId, String clientSecret, String apiBase, Clock clock, TuyaUsageCounter usage, JsonMapper json, RestClient rest) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.configured = !this.clientId.isEmpty() && !this.clientSecret.isEmpty() && apiBase != null && !apiBase.isBlank();
        this.clock = clock;
        this.usage = usage;
        this.json = json;
        this.rest = rest;
    }

    private static RestClient buildRestClient(String apiBase) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        String base = apiBase == null ? "" : apiBase.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (!base.isEmpty()) {
            builder.baseUrl(base);
        }
        return builder.build();
    }

    public boolean isConfigured() {
        return configured;
    }

    // ---- typed calls -----------------------------------------------------------------------

    public record TuyaDevice(String id, String name, String category, String productName, boolean online, boolean sub) {}

    public record TuyaDeviceState(boolean online, Map<String, JsonNode> status) {
        /** The boolean status under {@code code}, or null when the device does not report it as a boolean. */
        public Boolean booleanStatus(String code) {
            JsonNode node = status.get(code);
            return node != null && node.isBoolean() ? node.asBoolean() : null;
        }
    }

    public record TuyaRemote(String remoteId, String name) {}

    /** Every device of the project (the add screen). Reads only id, names, category, flags - never {@code localKey}. */
    public List<TuyaDevice> listDevices() {
        List<TuyaDevice> devices = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String lastId = null;
        for (int page = 0; page < DEVICE_MAX_PAGES; page++) {
            Map<String, String> query = new TreeMap<>();
            query.put("page_size", String.valueOf(DEVICE_PAGE_SIZE));
            if (lastId != null) {
                query.put("last_id", lastId);
            }
            JsonNode result = call("GET", "/v2.0/cloud/thing/device", query, null);
            JsonNode items = result.isArray() ? result : result.path("list");
            int added = 0;
            String last = null;
            for (JsonNode item : items) {
                String id = text(item, "id");
                if (id == null) {
                    continue;
                }
                last = id;
                if (!seen.add(id)) {
                    continue;
                }
                added++;
                String custom = text(item, "customName");
                String name = custom != null && !custom.isBlank() ? custom : text(item, "name");
                devices.add(new TuyaDevice(
                        id,
                        name == null ? id : name,
                        text(item, "category") == null ? "" : text(item, "category"),
                        text(item, "productName"),
                        item.path("isOnline").asBoolean(false),
                        item.path("sub").asBoolean(false)));
            }
            if (added == 0 || items.size() < DEVICE_PAGE_SIZE) {
                break;
            }
            lastId = last;
        }
        return devices;
    }

    /** One call: whether the device is online and its current status codes. */
    public TuyaDeviceState deviceState(String deviceId) {
        JsonNode result = call("GET", "/v1.0/devices/" + deviceId, Map.of(), null);
        Map<String, JsonNode> status = new LinkedHashMap<>();
        for (JsonNode entry : result.path("status")) {
            String code = text(entry, "code");
            if (code != null) {
                status.put(code, entry.path("value"));
            }
        }
        return new TuyaDeviceState(result.path("online").asBoolean(false), status);
    }

    /** Sends one command code to a (relay) device. */
    public void sendCommand(String deviceId, String code, Object value) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("code", code);
        command.put("value", value);
        call("POST", "/v1.0/devices/" + deviceId + "/commands", Map.of(), Map.of("commands", List.of(command)));
    }

    /** The air-conditioner scene command of an IR hub: the whole setting in one call. */
    public void sendAirConditionerCommand(String infraredId, String remoteId, int power, int mode, int temp, int wind) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("power", power);
        body.put("mode", mode);
        body.put("temp", temp);
        body.put("wind", wind);
        call("POST", "/v2.0/infrareds/" + infraredId + "/air-conditioners/" + remoteId + "/scenes/command", Map.of(), body);
    }

    /** The remotes learned by an IR hub. */
    public List<TuyaRemote> listRemotes(String infraredId) {
        JsonNode result = call("GET", "/v2.0/infrareds/" + infraredId + "/remotes", Map.of(), null);
        List<TuyaRemote> remotes = new ArrayList<>();
        for (JsonNode item : result.isArray() ? result : result.path("list")) {
            String id = text(item, "remote_id");
            if (id != null) {
                remotes.add(new TuyaRemote(id, text(item, "remote_name")));
            }
        }
        return remotes;
    }

    // ---- request machinery -----------------------------------------------------------------

    /** Signed call; returns Tuya's {@code result}. Fetches a new token once if Tuya says ours is stale. */
    private JsonNode call(String method, String path, Map<String, String> query, Object body) {
        if (!configured) {
            throw new TuyaException(TuyaException.Kind.NOT_CONFIGURED, MSG_NOT_CONFIGURED);
        }
        String bodyJson = body == null ? "" : json.writeValueAsString(body);
        String url = urlWithQuery(path, query);
        for (int attempt = 0; ; attempt++) {
            JsonNode response = execute(method, url, bodyJson, token());
            if (response.path("success").asBoolean(false)) {
                return response.path("result");
            }
            long code = response.path("code").asLong(0);
            if (code == TOKEN_EXPIRED && attempt == 0) {
                log.warn("Tuya said the access token is stale (code {}); fetching a new one", code);
                forgetToken();
                continue;
            }
            throw translate(code, response.path("msg").asString(""), method, path);
        }
    }

    private JsonNode execute(String method, String urlWithQuery, String bodyJson, String token) {
        String t = String.valueOf(clock.millis());
        String nonce = UUID.randomUUID().toString();
        String sign = sign(clientId, clientSecret, token, t, nonce, method, bodyJson, urlWithQuery);
        usage.increment();
        try {
            RestClient.RequestBodySpec spec = rest.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(urlWithQuery)
                    .header("client_id", clientId)
                    .header("sign", sign)
                    .header("t", t)
                    .header("sign_method", "HMAC-SHA256")
                    .header("nonce", nonce);
            if (token != null) {
                spec.header("access_token", token);
            }
            if (!bodyJson.isEmpty()) {
                spec.contentType(MediaType.APPLICATION_JSON).body(bodyJson);
            }
            String raw = spec.retrieve().body(String.class);
            return json.readTree(raw == null ? "{}" : raw);
        } catch (RestClientResponseException e) {
            log.warn("Tuya {} {} answered HTTP {}", method, pathOnly(urlWithQuery), e.getStatusCode().value());
            if (e.getStatusCode().is5xxServerError()) {
                throw new TuyaException(TuyaException.Kind.NETWORK, MSG_NETWORK);
            }
            throw new TuyaException(TuyaException.Kind.REJECTED, MSG_REJECTED);
        } catch (ResourceAccessException e) {
            log.warn("Tuya {} {} unreachable: {}", method, pathOnly(urlWithQuery), e.getMessage());
            throw new TuyaException(TuyaException.Kind.NETWORK, MSG_NETWORK);
        } catch (tools.jackson.core.JacksonException e) {
            log.warn("Tuya {} {} answered something that is not JSON", method, pathOnly(urlWithQuery));
            throw new TuyaException(TuyaException.Kind.NETWORK, MSG_NETWORK);
        }
    }

    private TuyaException translate(long code, String msg, String method, String path) {
        // The raw code and Tuya's own text go to the log only; the screen gets a plain sentence.
        log.warn("Tuya {} {} refused: code={} msg=\"{}\"", method, path, code, msg);
        if (code == API_NOT_SUBSCRIBED) {
            return new TuyaException(TuyaException.Kind.UNAVAILABLE, MSG_API_NOT_SUBSCRIBED);
        }
        if (code == SUBSCRIPTION_EXPIRED || code == TOKEN_EXPIRED) {
            return new TuyaException(TuyaException.Kind.UNAVAILABLE, MSG_UNAVAILABLE);
        }
        return new TuyaException(TuyaException.Kind.REJECTED, MSG_REJECTED);
    }

    private String token() {
        long now = clock.millis();
        String current = accessToken;
        if (current != null && now < accessTokenExpiresAtMs - TOKEN_EARLY_REFRESH_MS) {
            return current;
        }
        synchronized (tokenLock) {
            now = clock.millis();
            if (accessToken != null && now < accessTokenExpiresAtMs - TOKEN_EARLY_REFRESH_MS) {
                return accessToken;
            }
            JsonNode response = execute("GET", urlWithQuery("/v1.0/token", Map.of("grant_type", "1")), "", null);
            if (!response.path("success").asBoolean(false)) {
                log.warn("Tuya refused the token request: code={} msg=\"{}\"", response.path("code").asLong(0), response.path("msg").asString(""));
                throw new TuyaException(TuyaException.Kind.UNAVAILABLE, MSG_UNAVAILABLE);
            }
            JsonNode result = response.path("result");
            String fresh = result.path("access_token").asString("");
            if (fresh.isEmpty()) {
                log.warn("Tuya's token answer had no access_token");
                throw new TuyaException(TuyaException.Kind.UNAVAILABLE, MSG_UNAVAILABLE);
            }
            accessToken = fresh;
            accessTokenExpiresAtMs = clock.millis() + result.path("expire_time").asLong(7200) * 1000L;
            return fresh;
        }
    }

    private void forgetToken() {
        synchronized (tokenLock) {
            accessToken = null;
            accessTokenExpiresAtMs = 0;
        }
    }

    // ---- signing ---------------------------------------------------------------------------

    /** Path plus the query sorted by key, exactly the string Tuya signs. */
    static String urlWithQuery(String path, Map<String, String> query) {
        if (query.isEmpty()) {
            return path;
        }
        StringBuilder sb = new StringBuilder(path).append('?');
        boolean first = true;
        for (Map.Entry<String, String> e : new TreeMap<>(query).entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    /**
     * Tuya's signature: {@code HMAC-SHA256(secret, client_id + [access_token] + t + nonce +
     * stringToSign)} in upper-case hex, where {@code stringToSign = METHOD \n SHA256(body) \n
     * signedHeaders(empty) \n url-with-sorted-query}. {@code accessToken} is null for the token request.
     */
    static String sign(
            String clientId, String secret, String accessToken, String t, String nonce, String method, String body, String urlWithQuery) {
        String stringToSign = method + "\n" + sha256Hex(body == null ? "" : body) + "\n\n" + urlWithQuery;
        String message = clientId + (accessToken == null ? "" : accessToken) + t + nonce + stringToSign;
        return hmacSha256Hex(secret, message).toUpperCase();
    }

    private static String sha256Hex(String value) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String pathOnly(String urlWithQuery) {
        int q = urlWithQuery.indexOf('?');
        return q < 0 ? urlWithQuery : urlWithQuery.substring(0, q);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.asString() : null;
    }
}
