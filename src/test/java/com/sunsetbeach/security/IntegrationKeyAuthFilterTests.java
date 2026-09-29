package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** The integration key filter on its own: off when unconfigured, and scoped to the import path under the real /api context path. */
class IntegrationKeyAuthFilterTests {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest request(String contextPath, String path, String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", contextPath + path);
        request.setContextPath(contextPath);
        if (key != null) {
            request.addHeader(IntegrationKeyAuthFilter.HEADER, key);
        }
        return request;
    }

    private static boolean authenticates(IntegrationKeyAuthFilter filter, MockHttpServletRequest request) throws Exception {
        SecurityContextHolder.clearContext();
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() == IntegrationPrincipal.SITEMINDER;
    }

    @Test
    void matchingKeyOnTheImportPath_authenticatesAsSiteMinder_underTheApiContextPath() throws Exception {
        assertThat(authenticates(new IntegrationKeyAuthFilter("secret"), request("/api", IntegrationKeyAuthFilter.IMPORT_PATH, "secret"))).isTrue();
    }

    @Test
    void unconfiguredKey_turnsTheIntegrationOff() throws Exception {
        assertThat(authenticates(new IntegrationKeyAuthFilter(""), request("/api", IntegrationKeyAuthFilter.IMPORT_PATH, ""))).isFalse();
        assertThat(authenticates(new IntegrationKeyAuthFilter(null), request("/api", IntegrationKeyAuthFilter.IMPORT_PATH, "anything"))).isFalse();
    }

    @Test
    void rightKeyOnAnyOtherPath_orWrongKey_authenticatesNothing() throws Exception {
        IntegrationKeyAuthFilter filter = new IntegrationKeyAuthFilter("secret");
        assertThat(authenticates(filter, request("/api", "/bookings", "secret"))).isFalse();
        assertThat(authenticates(filter, request("/api", IntegrationKeyAuthFilter.IMPORT_PATH + "/x", "secret"))).isFalse();
        assertThat(authenticates(filter, request("/api", IntegrationKeyAuthFilter.IMPORT_PATH, "Secret"))).isFalse();
        assertThat(authenticates(filter, request("/api", IntegrationKeyAuthFilter.IMPORT_PATH, null))).isFalse();
    }
}
