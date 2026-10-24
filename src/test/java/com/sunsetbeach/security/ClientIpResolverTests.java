package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTests {

    @Test
    void aCallerWrittenFirstHop_isIgnored_theProxysOwnEntryCounts() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void aSingleValue_fromTheNextServer_isUsedAsIs() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 198.51.100.2 ");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void noHeader_fallsBackToTheConnection() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("10.0.0.5");
    }
}
