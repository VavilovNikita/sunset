package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.model.GuestAccountRegisterInput;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.GuestAccountAuthRateLimiter;
import com.sunsetbeach.security.GuestAccountLoginRateLimiter;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.mapper.GuestAccountMapper;
import com.sunsetbeach.service.GuestAccountService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises {@code /guest-auth/**} through the real SecurityConfig/GuestJwtService (public, no
 * token needed) with {@link GuestAccountService} mocked - the actual register/verify/resend/login
 * business logic (dedup, real password hashing, real DB uniqueness) is covered instead by
 * {@code GuestAccountServiceTests} against the real DB. This class covers the boundary this
 * layer alone owns: generic response shapes, status-code mapping, and rate limiting.
 */
@WebMvcTest(controllers = {GuestAccountAuthController.class})
@Import({SecurityConfig.class, JwtService.class, GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class,
        GuestAccountMapper.class, GuestAccountAuthRateLimiter.class, GuestAccountLoginRateLimiter.class})
class GuestAccountAuthControllerTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";
    private static final String GUEST_JWT_SECRET = "test-guest-jwt-secret-at-least-32-bytes!!";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestAccountService guestAccountService;

    @MockitoBean
    private GuestAccountRepository guestAccountRepository;

    @MockitoBean
    private RoomRepository roomRepository;

    // SecurityConfig's filter chain also wires the staff JwtAuthFilter (see that class) - never
    // exercised by this guest-only suite, but the bean it depends on must still resolve for the
    // context to start.
    @MockitoBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-ttl-days", () -> "7");
        registry.add("app.security.guest-jwt-secret", () -> GUEST_JWT_SECRET);
        registry.add("app.security.guest-jwt-ttl-days", () -> "30");
    }

    private GuestAccountEntity verifiedAccount() {
        GuestAccountEntity entity = new GuestAccountEntity();
        entity.setId("guest-1");
        entity.setEmail("guest@example.com");
        entity.setEmailVerifiedAt(LocalDateTime.now());
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
        return entity;
    }

    @Test
    void register_alwaysReturnsGenericMessage_regardlessOfServiceOutcome() throws Exception {
        mockMvc.perform(post("/guest-auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new-guest@example.com\",\"password\":\"a-good-password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email isn't already registered, we've sent a verification link."))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(guestAccountService).register(any(GuestAccountRegisterInput.class));
    }

    // The per-IP and per-email threshold behavior of GuestAccountAuthRateLimiter itself is
    // covered by GuestAccountAuthRateLimiterTests (a fresh instance per test, no Spring context) -
    // not here. This class's rate limiter bean is a singleton shared across every test method via
    // Spring's test context cache, and unlike GuestAccountLoginRateLimiter's (ip, email) key (see
    // login_afterFiveFailuresFromSameIpAndEmail_isRateLimited below, which stays safe because
    // every test uses its own distinct email), this limiter's IP bucket has no such per-test
    // isolation - MockMvc requests all share the same synthetic remote address - so exhausting it
    // here would leak into every other test in this class depending on run order.

    @Test
    void resendVerification_alwaysReturnsGenericMessage() throws Exception {
        mockMvc.perform(post("/guest-auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(guestAccountService).resendVerification("someone@example.com");
    }

    @Test
    void verify_validToken_returnsTokenAndAccount() throws Exception {
        when(guestAccountService.verify("good-token")).thenReturn(verifiedAccount());

        mockMvc.perform(post("/guest-auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.account.email").value("guest@example.com"));
    }

    @Test
    void verify_expiredOrWrongToken_isBadRequest() throws Exception {
        when(guestAccountService.verify("bad-token")).thenThrow(new BadRequestException("Invalid or expired verification token"));

        mockMvc.perform(post("/guest-auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired verification token"));
    }

    @Test
    void login_correctCredentials_returnsTokenAndAccount() throws Exception {
        when(guestAccountService.login("guest@example.com", "correct-password1")).thenReturn(verifiedAccount());

        mockMvc.perform(post("/guest-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guest@example.com\",\"password\":\"correct-password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.account.email").value("guest@example.com"));
    }

    @Test
    void login_wrongPassword_isUnauthorizedWithGenericMessage() throws Exception {
        when(guestAccountService.login("guest@example.com", "wrong-password"))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/guest-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"guest@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void login_correctPasswordButUnverified_isForbiddenWithDistinctMessage() throws Exception {
        when(guestAccountService.login("unverified@example.com", "correct-password1"))
                .thenThrow(new ForbiddenException("Please verify your email before logging in."));

        mockMvc.perform(post("/guest-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unverified@example.com\",\"password\":\"correct-password1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Please verify your email before logging in."));
    }

    @Test
    void login_afterFiveFailuresFromSameIpAndEmail_isRateLimited() throws Exception {
        when(guestAccountService.login("rate-limited-guest@example.com", "wrong-password"))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/guest-auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"rate-limited-guest@example.com\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/guest-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rate-limited-guest@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
