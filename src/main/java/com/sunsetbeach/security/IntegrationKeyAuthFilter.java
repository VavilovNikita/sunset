package com.sunsetbeach.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the SiteMinder polling script by a shared secret in {@code X-Integration-Key}
 * (the {@code siteMinderIntegrationKey} scheme in openapi.yaml). The first system-to-system caller
 * this API has - everything else is a person with a staff or guest JWT - so it gets its own
 * credential rather than a staff login: a MANAGER token would give the script the whole MANAGER
 * surface, expire weekly, and make its writes look like a person's in the audit log.
 *
 * <p>Two things keep the key's reach to the one import route:
 * <ul>
 *   <li>it is only honored on {@link #IMPORT_PATH} - on any other path this filter does nothing,
 *       so the key can't satisfy the {@code authenticated()} rules other routes use;
 *   <li>it grants {@link IntegrationPrincipal#SITEMINDER_AUTHORITY}, never a {@code ROLE_}, so
 *       nothing in the role hierarchy applies to it.
 * </ul>
 * An unset key (the default) turns the integration off: nothing matches, every call is 401.
 * Compared as SHA-256 digests with {@link MessageDigest#isEqual}, so the comparison takes the same
 * time however much of a guess is right.
 */
public class IntegrationKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Integration-Key";
    static final String IMPORT_PATH = "/integrations/siteminder/reservations";

    private final byte[] expectedDigest;

    public IntegrationKeyAuthFilter(String configuredKey) {
        this.expectedDigest = configuredKey == null || configuredKey.isBlank() ? null : sha256(configuredKey);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (expectedDigest != null && presented != null && IMPORT_PATH.equals(pathWithinApp(request))
                && MessageDigest.isEqual(expectedDigest, sha256(presented))) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    IntegrationPrincipal.SITEMINDER, null, List.of(new SimpleGrantedAuthority(IntegrationPrincipal.SITEMINDER_AUTHORITY)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    /** The request path without the {@code /api} context path - the same form the security matchers use. */
    private static String pathWithinApp(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
