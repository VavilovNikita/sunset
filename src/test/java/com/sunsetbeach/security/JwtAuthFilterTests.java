package com.sunsetbeach.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit-level (no Spring context, no DB) coverage of the revocation check JwtAuthFilter now runs
 * on every request: a signature-valid, unexpired token is not enough on its own once a token can
 * be revoked out from under it - active/tokenVersion must also match the current User row. This
 * is the mechanism behind disabling a user, resetting their password, and changing role/password
 * all taking effect immediately instead of waiting out the token's remaining validity window.
 */
class JwtAuthFilterTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(JWT_SECRET, 7);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, userRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static UserEntity activeUser(int tokenVersion) {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setActive(true);
        entity.setTokenVersion(tokenVersion);
        return entity;
    }

    private void runFilter(String bearerHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (bearerHeader != null) {
            request.addHeader("Authorization", bearerHeader);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void validTokenAndActiveMatchingUser_authenticates() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(activeUser(0)));
        String token = jwtService.issue(new StaffPrincipal("user-1", "user@example.com", Role.CASHIER), 0);

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CASHIER");
    }

    @Test
    void noToken_doesNotAuthenticateButStillProceeds() throws Exception {
        runFilter(null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void userNoLongerExists_doesNotAuthenticate() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        String token = jwtService.issue(new StaffPrincipal("user-1", "user@example.com", Role.CASHIER), 0);

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void disabledUser_doesNotAuthenticate() throws Exception {
        UserEntity disabled = activeUser(0);
        disabled.setActive(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(disabled));
        String token = jwtService.issue(new StaffPrincipal("user-1", "user@example.com", Role.CASHIER), 0);

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void staleTokenVersion_doesNotAuthenticate() throws Exception {
        // Simulates a token issued before a password change, role change, or admin reset bumped
        // tokenVersion from 0 to 1 - the token itself is unexpired and correctly signed.
        when(userRepository.findById("user-1")).thenReturn(Optional.of(activeUser(1)));
        String token = jwtService.issue(new StaffPrincipal("user-1", "user@example.com", Role.CASHIER), 0);

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenWithoutTokenVersionClaim_treatedAsVersionZero() throws Exception {
        // A token issued via the single-arg issue() overload (used by call sites that don't
        // track tokenVersion, e.g. some tests) - JwtService.parse() defaults a missing claim to
        // 0, which matches every account's starting value.
        when(userRepository.findById("user-1")).thenReturn(Optional.of(activeUser(0)));
        String token = jwtService.issue(new StaffPrincipal("user-1", "user@example.com", Role.CASHIER));

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
