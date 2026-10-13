package com.sunsetbeach.tuya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Tuya HTTP client against a mocked server: the signature (checked against values computed
 * independently in Python, not by this class), the token cache and its refresh, the one retry on a
 * stale token, the translation of Tuya failures into messages for the screen, and the call counter.
 */
class TuyaClientTests {

    private static final String BASE = "https://tuya.test";
    private static final String TOKEN_URL = BASE + "/v1.0/token?grant_type=1";
    private static final String TOKEN_OK = """
            {"success":true,"t":1,"result":{"access_token":"tok-A","expire_time":7200,"refresh_token":"r","uid":"u"}}""";
    private static final String TOKEN_OK_B = """
            {"success":true,"t":1,"result":{"access_token":"tok-B","expire_time":7200,"refresh_token":"r","uid":"u"}}""";
    private static final String DEVICE_OK = """
            {"success":true,"t":1,"result":{"id":"d1","online":true,"status":[{"code":"switch_1","value":true},{"code":"countdown_1","value":0}]}}""";

    private MutableClock clock;
    private TuyaUsageCounter usage;
    private MockRestServiceServer server;
    private TuyaClient client;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-13T03:00:00Z"));
        usage = mock(TuyaUsageCounter.class);
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TuyaClient("cid", "sec", BASE, clock, usage, JsonMapper.builder().build(), builder.build());
    }

    @Test
    void signature_matchesValuesComputedIndependently() {
        assertThat(TuyaClient.sign("cid", "sec", "tok", "1700000000000", "n-1", "POST",
                        "{\"commands\":[{\"code\":\"switch_1\",\"value\":true}]}", "/v1.0/devices/d1/commands"))
                .isEqualTo("2642289E25A532B4F19104440D3C0E4A0FEF04F0CD2E9C63A2B1621B85329629");
        // The token request has no access token and an empty body.
        assertThat(TuyaClient.sign("cid", "sec", null, "1700000000000", "n-1", "GET", "", "/v1.0/token?grant_type=1"))
                .isEqualTo("43C401D4665E48AF15D7B2875CB0656AEEE08F74C80201D4C957F75882DF2F88");
        // Query parameters are signed sorted by key.
        assertThat(TuyaClient.urlWithQuery("/v2.0/cloud/thing/device", Map.of("page_size", "20", "last_id", "x")))
                .isEqualTo("/v2.0/cloud/thing/device?last_id=x&page_size=20");
        assertThat(TuyaClient.sign("cid", "sec", "tok", "1700000000000", "n-1", "GET", "",
                        TuyaClient.urlWithQuery("/v2.0/cloud/thing/device", Map.of("page_size", "20", "last_id", "x"))))
                .isEqualTo("55B4E280A6E5A0F8C12F1302F7F99D525628194BE49576CEBE77362D3CE10AAD");
    }

    @Test
    void everyRequest_carriesASignatureThatRecomputesFromItsOwnHeaders() {
        server.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.GET)).andExpect(signedWith(null, "GET", ""))
                .andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-A")).andExpect(signedWith("tok-A", "GET", ""))
                .andExpect(header("sign_method", "HMAC-SHA256")).andExpect(header("client_id", "cid"))
                .andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));

        client.deviceState("d1");

        server.verify();
    }

    @Test
    void deviceState_readsOnlineAndBooleanSwitch() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));

        TuyaClient.TuyaDeviceState state = client.deviceState("d1");

        assertThat(state.online()).isTrue();
        assertThat(state.booleanStatus("switch_1")).isTrue();
        assertThat(state.booleanStatus("countdown_1")).as("a number is not a switch").isNull();
        assertThat(state.booleanStatus("missing")).isNull();
    }

    @Test
    void token_isReusedWhileValid_andRefreshedBeforeItExpires() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-A"))
                .andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-A"))
                .andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK_B, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-B"))
                .andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));

        client.deviceState("d1");
        clock.advance(Duration.ofSeconds(7200 - 61)); // still outside the 60 s early-refresh window
        client.deviceState("d1");
        // 59 s before expiry is inside the window: a new token is fetched first, not a failed call.
        clock.advance(Duration.ofSeconds(2));
        client.deviceState("d1");

        server.verify();
    }

    @Test
    void staleTokenAnswer_fetchesANewTokenAndRetriesOnce() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-A"))
                .andRespond(withSuccess("{\"success\":false,\"code\":1010,\"msg\":\"token invalid\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK_B, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andExpect(header("access_token", "tok-B"))
                .andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));

        assertThat(client.deviceState("d1").online()).isTrue();

        server.verify();
    }

    @Test
    void sendCommand_postsTheSwitchCommandBody() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1/commands")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"commands\":[{\"code\":\"switch_1\",\"value\":true}]}"))
                .andExpect(signedWith("tok-A", "POST", "{\"commands\":[{\"code\":\"switch_1\",\"value\":true}]}"))
                .andRespond(withSuccess("{\"success\":true,\"result\":true,\"t\":1}", MediaType.APPLICATION_JSON));

        client.sendCommand("d1", "switch_1", true);

        server.verify();
    }

    @Test
    void airConditionerCommand_usesTheIrSceneEndpointWithAllFourFields() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2.0/infrareds/hub1/air-conditioners/rem1/scenes/command")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"power\":1,\"mode\":0,\"temp\":24,\"wind\":2}", true))
                .andRespond(withSuccess("{\"success\":true,\"result\":true,\"t\":1}", MediaType.APPLICATION_JSON));

        client.sendAirConditionerCommand("hub1", "rem1", 1, 0, 24, 2);

        server.verify();
    }

    @Test
    void listDevices_usesTheCustomNameAndCarriesNoSecrets() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2.0/cloud/thing/device?page_size=20")).andRespond(withSuccess("""
                {"success":true,"t":1,"result":[
                 {"id":"a1","name":"WiFi Breaker T 2","customName":"roof fan 1","category":"kg","productName":"WiFi Breaker T","isOnline":true,"sub":false,"localKey":"SECRET-LOCAL-KEY"},
                 {"id":"a2","name":"Air Conditioner","customName":"","category":"qt","isOnline":false,"sub":true}]}""", MediaType.APPLICATION_JSON));

        List<TuyaClient.TuyaDevice> devices = client.listDevices();

        assertThat(devices).extracting(TuyaClient.TuyaDevice::name).containsExactly("roof fan 1", "Air Conditioner");
        assertThat(devices.get(0).online()).isTrue();
        assertThat(devices.get(1).sub()).isTrue();
        assertThat(devices.toString()).doesNotContain("SECRET-LOCAL-KEY");
    }

    @Test
    void apiNotSubscribed_isSaidPlainly() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v2.0/infrareds/hub1/remotes")).andRespond(withSuccess(
                "{\"code\":28841101,\"msg\":\"No permissions. This API is not subscribed.\",\"success\":false,\"t\":1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.listRemotes("hub1"))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(TuyaException.Kind.UNAVAILABLE);
                    assertThat(e.getMessage()).isEqualTo(TuyaClient.MSG_API_NOT_SUBSCRIBED).doesNotContain("28841101");
                });
    }

    @Test
    void expiredSubscription_saysToCheckTheSubscription() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andRespond(withSuccess(
                "{\"code\":28841002,\"msg\":\"No permissions. Your subscription to cloud development plan has expired.\",\"success\":false,\"t\":1}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.deviceState("d1"))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getMessage())
                            .isEqualTo("Tuya integration is unavailable — check the IoT Core subscription (extend the trial period at iot.tuya.com)");
                    assertThat(e.isIntegrationDown()).isTrue();
                });
    }

    @Test
    void aCodeNobodyHasSeen_isRejected_withoutGuessingAndWithoutTheRawCode() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1/commands")).andRespond(withSuccess(
                "{\"code\":99999,\"msg\":\"something new\",\"success\":false,\"t\":1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.sendCommand("d1", "switch_1", true))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(TuyaException.Kind.REJECTED);
                    assertThat(e.getMessage()).doesNotContain("99999").doesNotContain("something new");
                    assertThat(e.isIntegrationDown()).isFalse();
                });
    }

    @Test
    void refusedTokenRequest_meansTheIntegrationIsDown() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
                "{\"code\":1004,\"msg\":\"sign invalid\",\"success\":false,\"t\":1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.deviceState("d1"))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(TuyaException.Kind.UNAVAILABLE);
                    assertThat(e.getMessage()).isEqualTo(TuyaClient.MSG_UNAVAILABLE);
                });
    }

    @Test
    void serverError_isANetworkFailure() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.deviceState("d1"))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(TuyaException.Kind.NETWORK);
                    assertThat(e.isIntegrationDown()).isFalse();
                });
    }

    @Test
    void withoutKeys_nothingIsSentAndNothingIsCounted() {
        TuyaClient unconfigured = new TuyaClient("", "", "", clock, usage, JsonMapper.builder().build(), RestClient.builder().build());

        assertThat(unconfigured.isConfigured()).isFalse();
        assertThatThrownBy(() -> unconfigured.deviceState("d1"))
                .isInstanceOfSatisfying(TuyaException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(TuyaException.Kind.NOT_CONFIGURED);
                    assertThat(e.isIntegrationDown()).isTrue();
                });
        verifyNoInteractions(usage);
    }

    @Test
    void everyHttpAttempt_isCounted_tokenRequestIncluded() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andRespond(withSuccess(DEVICE_OK, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1.0/devices/d1")).andRespond(withSuccess(
                "{\"code\":99999,\"msg\":\"no\",\"success\":false,\"t\":1}", MediaType.APPLICATION_JSON));

        client.deviceState("d1");
        assertThatThrownBy(() -> client.deviceState("d1")).isInstanceOf(TuyaException.class);

        verify(usage, times(3)).increment(); // token + ok + refused: a refused call still costs quota
    }

    /** Recomputes the signature from the request's own t / nonce / token headers and compares. */
    private static RequestMatcher signedWith(String token, String httpMethod, String body) {
        return request -> {
            String t = request.getHeaders().getFirst("t");
            String nonce = request.getHeaders().getFirst("nonce");
            String url = request.getURI().getRawPath() + (request.getURI().getRawQuery() == null ? "" : "?" + request.getURI().getRawQuery());
            String expected = TuyaClient.sign("cid", "sec", token, t, nonce, httpMethod, body, url);
            assertThat(request.getHeaders().getFirst("sign")).isEqualTo(expected);
        };
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Bangkok");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
