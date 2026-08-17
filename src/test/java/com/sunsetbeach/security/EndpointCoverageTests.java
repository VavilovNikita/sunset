package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards against the failure mode that hit {@code /room-units}, then {@code /rooms}/{@code
 * /pricing}/{@code /bookings}/{@code /availability}: a new (or overlooked) path in {@code
 * openapi.yaml} silently gets no {@code requestMatchers(...)} entry in {@link SecurityConfig}
 * and falls through to the final {@code anyRequest().authenticated()} - available to every
 * staff role regardless of what the contract documents, discovered only by someone noticing in
 * review rather than by a test failing.
 *
 * <p>This is deliberately NOT a check of which role each path requires (that's
 * {@link com.sunsetbeach.controller.PosRoleHierarchyTests}'s job, and pinning exact roles here
 * would make this test as brittle as the bug it's meant to catch - a new grant added correctly
 * would still make it fail). It only asks: for every (method, path) pair {@code openapi.yaml}
 * declares, is there a {@code SecurityConfig} rule that claims it *before* the {@code
 * anyRequest()} catch-all? A straight string comparison against {@code SecurityConfig}'s source
 * can't answer that - the rules are Ant/path-pattern templates ({@code "/room-units/*"},
 * {@code "/orders/**"}) matched against concrete request paths, and openapi.yaml's own paths
 * are templates too ({@code "/bookings/{id}"}). Comparing rule text to path text would mean
 * reimplementing Spring Security's own pattern-matching semantics just to keep two
 * representations in sync by hand - exactly the kind of drift this test exists to prevent.
 *
 * <p>Instead, this reaches into the actual {@link SecurityFilterChain} bean Spring Security
 * builds from {@link SecurityConfig} and asks it directly: for a concrete request built from
 * each openapi.yaml path (path variables substituted with a placeholder segment), which {@code
 * requestMatchers(...)} entry matches first? {@code authorizeHttpRequests(...)} compiles to a
 * {@link RequestMatcherDelegatingAuthorizationManager} holding its rules, in declaration order,
 * as a private {@code mappings} list - reflection is the only way to see that order at all
 * (there is no public API for it, because "which rule fired" is normally something only the
 * framework needs to know). If the first match is the {@code anyRequest()} rule
 * ({@link AnyRequestMatcher#INSTANCE}), the path has no dedicated rule and the test fails,
 * listing every such path/method so the fix is a five-second diff, not an investigation.
 */
@SpringBootTest
class EndpointCoverageTests {

    private static final Set<String> HTTP_METHOD_KEYS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    private SecurityFilterChain securityFilterChain;

    private record Route(String method, String templatePath) {
        String concretePath() {
            // Path variables can't be matched literally against Ant/PathPattern templates, and their
            // actual value never affects which security rule applies - any placeholder segment works.
            return templatePath.replaceAll("\\{[^}]+\\}", "x");
        }
    }

    @Test
    void everyOpenApiPathHasAnExplicitSecurityRuleBeforeTheCatchAll() throws Exception {
        List<RequestMatcherEntry> mappings = extractMappings();

        // Sanity check on the guard itself: if the last rule ever stops being the anyRequest()
        // catch-all, "first match isn't anyRequest()" stops meaning anything, and this test would
        // pass vacuously without checking a thing.
        assertThat(mappings.get(mappings.size() - 1).getRequestMatcher())
                .as("SecurityConfig's last authorizeHttpRequests() rule must be anyRequest() for this guard to mean anything")
                .isEqualTo(AnyRequestMatcher.INSTANCE);

        List<String> uncovered = new ArrayList<>();
        for (Route route : openApiRoutes()) {
            RequestMatcher matched = firstMatch(mappings, route.method(), route.concretePath());
            if (matched == null || matched.equals(AnyRequestMatcher.INSTANCE)) {
                uncovered.add(route.method() + " " + route.templatePath());
            }
        }

        assertThat(uncovered)
                .as("Every path in openapi.yaml must be claimed by a specific SecurityConfig rule "
                        + "before the final anyRequest() catch-all - add an explicit requestMatchers(...) entry for each")
                .isEmpty();
    }

    /** Proves the guard can actually fail: a path outside the contract must fall through, or the mechanism above is broken. */
    @Test
    void aPathOutsideTheContractDoesFallThroughToTheCatchAll() throws Exception {
        List<RequestMatcherEntry> mappings = extractMappings();
        RequestMatcher matched = firstMatch(mappings, "GET", "/this-path-is-not-declared-in-openapi-yaml");
        assertThat(matched).isEqualTo(AnyRequestMatcher.INSTANCE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<RequestMatcherEntry> extractMappings() throws Exception {
        AuthorizationFilter authorizationFilter = securityFilterChain.getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No AuthorizationFilter in the SecurityFilterChain"));

        Object manager = unwrapToDelegatingManager(authorizationFilter.getAuthorizationManager());
        // No public accessor exists for the ordered rule list - authorize() is designed to be called,
        // not introspected. This field name/type is part of Spring Security's internal implementation of
        // authorizeHttpRequests(), not a stable public API - if it ever changes, this test fails loudly
        // (NoSuchFieldException) rather than silently stop guarding anything, which is the acceptable
        // failure mode for a reflection-based check like this one.
        Field mappingsField = RequestMatcherDelegatingAuthorizationManager.class.getDeclaredField("mappings");
        mappingsField.setAccessible(true);
        return (List<RequestMatcherEntry>) mappingsField.get(manager);
    }

    /**
     * Actuator's Micrometer auto-instrumentation wraps the real manager in an
     * {@code ObservationAuthorizationManager} for metrics - unwrap any number of such
     * decorators (each exposes its wrapped manager as a private {@code delegate} field, with no
     * public accessor) to reach the actual {@link RequestMatcherDelegatingAuthorizationManager}
     * {@code authorizeHttpRequests(...)} built.
     */
    private static Object unwrapToDelegatingManager(Object manager) throws Exception {
        Object current = manager;
        while (!(current instanceof RequestMatcherDelegatingAuthorizationManager)) {
            Field delegateField;
            try {
                delegateField = current.getClass().getDeclaredField("delegate");
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(
                        "Expected to unwrap down to RequestMatcherDelegatingAuthorizationManager, got stuck at "
                                + current.getClass() + " with no 'delegate' field to unwrap further",
                        e);
            }
            delegateField.setAccessible(true);
            current = delegateField.get(current);
        }
        return current;
    }

    @SuppressWarnings("rawtypes")
    private RequestMatcher firstMatch(List<RequestMatcherEntry> mappings, String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        for (Object mapping : mappings) {
            RequestMatcherEntry entry = (RequestMatcherEntry) mapping;
            RequestMatcher matcher = entry.getRequestMatcher();
            if (matcher.matches(request)) {
                return matcher;
            }
        }
        return null;
    }

    /** One entry per (method, path) operation declared under `paths:` in openapi.yaml. */
    @SuppressWarnings("unchecked")
    private static List<Route> openApiRoutes() throws Exception {
        List<Route> routes = new ArrayList<>();
        try (InputStream in = Files.newInputStream(Path.of("openapi.yaml"))) {
            Map<String, Object> spec = new Yaml().load(in);
            Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String templatePath = pathEntry.getKey();
                Map<String, Object> operations = (Map<String, Object>) pathEntry.getValue();
                for (String key : operations.keySet()) {
                    if (HTTP_METHOD_KEYS.contains(key)) {
                        routes.add(new Route(key.toUpperCase(Locale.ROOT), templatePath));
                    }
                }
            }
        }
        return routes;
    }
}
