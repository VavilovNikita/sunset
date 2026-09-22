package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.mapper.GuestAccountMapper;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.GuestPrincipal;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.service.GuestAccountService;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
 * Exercises {@code /guest/**} through the real SecurityConfig/GuestJwtService/GuestJwtAuthFilter
 * with {@link GuestAccountService}/{@link GuestAccountRepository} mocked - the tokenVersion
 * revocation check (this class's main point) lives in GuestJwtAuthFilter itself, so it must run
 * for real; the DB it reads from is mocked.
 */
@WebMvcTest(controllers = {GuestAccountController.class})
@Import({SecurityConfig.class, JwtService.class, GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class, GuestAccountMapper.class})
class GuestAccountControllerTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";
    private static final String GUEST_JWT_SECRET = "test-guest-jwt-secret-at-least-32-bytes!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GuestJwtService guestJwtService;

    @MockitoBean
    private GuestAccountRepository guestAccountRepository;

    @MockitoBean
    private GuestAccountService guestAccountService;

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

    private GuestAccountEntity verifiedAccount(String id, int tokenVersion) {
        GuestAccountEntity entity = new GuestAccountEntity();
        entity.setId(id);
        entity.setEmail("guest@example.com");
        entity.setEmailVerifiedAt(LocalDateTime.now());
        entity.setTokenVersion(tokenVersion);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
        return entity;
    }

    /** GuestJwtAuthFilter re-checks the account row on every request - see that class's own javadoc. */
    @BeforeEach
    void stubVerifiedAccountForAnyId() {
        when(guestAccountRepository.findById(anyString())).thenAnswer(invocation -> Optional.of(verifiedAccount(invocation.getArgument(0), 0)));
    }

    private String tokenFor(String id, int tokenVersion) {
        return "Bearer " + guestJwtService.issue(new GuestPrincipal(id, "guest@example.com"), tokenVersion);
    }

    @Test
    void me_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/guest/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsAccount() throws Exception {
        when(guestAccountRepository.findById("guest-1")).thenReturn(Optional.of(verifiedAccount("guest-1", 0)));

        mockMvc.perform(get("/guest/me").header("Authorization", tokenFor("guest-1", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("guest@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void bookings_returnsThisGuestsHistory() throws Exception {
        GuestBookingView view = new GuestBookingView("booking-1", "Ocean View", "2026-01-01", "2026-01-05", "5000.00",
                com.sunsetbeach.model.BookingStatus.PAID, OffsetDateTime.now());
        when(guestAccountService.listBookings("guest@example.com")).thenReturn(List.of(view));

        mockMvc.perform(get("/guest/bookings").header("Authorization", tokenFor("guest-1", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("booking-1"))
                .andExpect(jsonPath("$[0].roomName").value("Ocean View"))
                .andExpect(jsonPath("$[0].paymentNote").doesNotExist());
    }

    @Test
    void changePassword_bumpsTokenVersion_oldTokenRejected_newTokenAccepted() throws Exception {
        GuestAccountEntity updated = verifiedAccount("guest-1", 1);
        when(guestAccountService.changePassword("guest-1", "old-password1", "brand-new-password1")).thenReturn(updated);

        // The @BeforeEach stub still has findById returning tokenVersion 0 at this point, so this
        // request (bearing the version-0 token) passes GuestJwtAuthFilter and reaches the mocked
        // service, which reports the change as having bumped the stored row to version 1.
        String oldToken = tokenFor("guest-1", 0);
        String response = mockMvc.perform(patch("/guest/password")
                        .header("Authorization", oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password1\",\"newPassword\":\"brand-new-password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newToken = "Bearer " + objectMapper.readTree(response).get("token").asText();

        // Now the stored row is (as of this point on) at version 1 - simulating the real DB state
        // after the change actually landed. GuestJwtAuthFilter re-checks this on every request, so
        // the old token (still claiming version 0) no longer matches.
        when(guestAccountRepository.findById("guest-1")).thenReturn(Optional.of(updated));
        mockMvc.perform(get("/guest/me").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());

        // Fresh token from the response keeps working.
        mockMvc.perform(get("/guest/me").header("Authorization", newToken))
                .andExpect(status().isOk());
    }
}
