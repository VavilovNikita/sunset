package com.sunsetbeach.security;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the actual safety boundary the whole GuestAccount design rests on (see the
 * GuestAccountAuth tag's own openapi.yaml description and CLAUDE.md's Authorization section): a
 * staff JWT (signed with {@code app.security.jwt-secret}) and a guest JWT (signed with
 * {@code app.security.guest-jwt-secret}) are verified by two independent filters against two
 * independent keys, so neither can ever satisfy the other's routes - not because of a role check
 * that happens to reject it today, but because the token literally never authenticates against
 * the wrong filter (see JwtAuthFilter/GuestJwtAuthFilter, which both run unconditionally on every
 * request). Driven through the real security filter chain via {@code @AutoConfigureMockMvc} on a
 * full context (same technique as {@code DateOnlyFieldsContractTests}), not a {@code @WebMvcTest}
 * slice, since the point is exercising both filters wired together for real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrossIdentityTokenRejectionTests extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private GuestJwtService guestJwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestAccountRepository guestAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String staffToken() {
        UserEntity user = new UserEntity();
        user.setName("Cross Token Test Admin");
        user.setEmail("cross-token-admin-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("whatever12"));
        user.setRole(Role.ADMIN);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        return "Bearer " + jwtService.issue(new StaffPrincipal(saved.getId(), saved.getEmail(), Role.ADMIN), saved.getTokenVersion());
    }

    private String guestToken() {
        GuestAccountEntity account = new GuestAccountEntity();
        account.setEmail("cross-token-guest-" + UUID.randomUUID() + "@example.com");
        account.setPasswordHash(passwordEncoder.encode("whatever12"));
        account.setEmailVerifiedAt(LocalDateTime.now());
        GuestAccountEntity saved = guestAccountRepository.saveAndFlush(account);
        return "Bearer " + guestJwtService.issue(new GuestPrincipal(saved.getId(), saved.getEmail()), saved.getTokenVersion());
    }

    @Test
    void guestToken_neverSatisfiesAStaffOnlyRoute() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", guestToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffToken_neverSatisfiesAGuestRoute() throws Exception {
        mockMvc.perform(get("/guest/me").header("Authorization", staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sanityCheck_guestTokenDoesSatisfyItsOwnRoute() throws Exception {
        mockMvc.perform(get("/guest/me").header("Authorization", guestToken()))
                .andExpect(status().isOk());
    }

    @Test
    void sanityCheck_staffTokenDoesSatisfyItsOwnRoute() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", staffToken()))
                .andExpect(status().isOk());
    }
}
