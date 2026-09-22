package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.model.GuestOrderItem;
import com.sunsetbeach.model.GuestOrderView;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.GuestPrincipal;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.RoomServiceOrderRateLimiter;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.service.RoomServiceOrderingService;
import java.time.LocalDateTime;
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
 * Exercises {@code /guest/orders/**} through the real SecurityConfig/GuestJwtService/
 * GuestJwtAuthFilter (a valid guest token is required) with {@link RoomServiceOrderingService}
 * mocked, and a real {@link RoomServiceOrderRateLimiter} bean (not mocked) so the 429 path is
 * actually exercised - same shape as {@link GuestAccountControllerTests}.
 */
@WebMvcTest(controllers = {GuestOrderController.class})
@Import({SecurityConfig.class, JwtService.class, GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class,
        RoomServiceOrderRateLimiter.class})
class GuestOrderControllerTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";
    private static final String GUEST_JWT_SECRET = "test-guest-jwt-secret-at-least-32-bytes!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GuestJwtService guestJwtService;

    @MockitoBean
    private RoomServiceOrderingService roomServiceOrderingService;

    @MockitoBean
    private GuestAccountRepository guestAccountRepository;

    // SecurityConfig's filter chain also wires the staff JwtAuthFilter - never exercised by this
    // guest-only suite, but the bean it depends on must still resolve for the context to start.
    @MockitoBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-ttl-days", () -> "7");
        registry.add("app.security.guest-jwt-secret", () -> GUEST_JWT_SECRET);
        registry.add("app.security.guest-jwt-ttl-days", () -> "30");
    }

    @BeforeEach
    void stubVerifiedAccountForAnyId() {
        when(guestAccountRepository.findById(anyString())).thenAnswer(invocation -> {
            GuestAccountEntity entity = new GuestAccountEntity();
            entity.setId(invocation.getArgument(0));
            entity.setEmail("guest@example.com");
            entity.setEmailVerifiedAt(LocalDateTime.now());
            entity.setTokenVersion(0);
            ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
            return Optional.of(entity);
        });
    }

    private String tokenFor(String id) {
        return "Bearer " + guestJwtService.issue(new GuestPrincipal(id, "guest@example.com"), 0);
    }

    private static GuestOrderView view() {
        return new GuestOrderView("order-1", OrderStatus.SENT, "Room 204", List.of(new GuestOrderItem("Mojito", 1, null, "150.00")), "150.00");
    }

    @Test
    void submit_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/guest/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"b1\",\"items\":[{\"menuItemId\":\"m1\",\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submit_withValidToken_returnsCreatedView() throws Exception {
        when(roomServiceOrderingService.submit(eq("guest@example.com"), eq("b1"), any())).thenReturn(view());

        mockMvc.perform(post("/guest/orders")
                        .header("Authorization", tokenFor("guest-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"b1\",\"items\":[{\"menuItemId\":\"m1\",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void addItems_withValidToken_returnsCreatedView() throws Exception {
        when(roomServiceOrderingService.addItems(eq("guest@example.com"), eq("order-1"), any())).thenReturn(view());

        mockMvc.perform(post("/guest/orders/order-1/items")
                        .header("Authorization", tokenFor("guest-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"menuItemId\":\"m1\",\"quantity\":1}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"));
    }

    @Test
    void getOrder_withValidToken_returnsView() throws Exception {
        when(roomServiceOrderingService.getById("guest@example.com", "order-1")).thenReturn(view());

        mockMvc.perform(get("/guest/orders/order-1").header("Authorization", tokenFor("guest-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationLabel").value("Room 204"));
    }

    @Test
    void getOrder_isNeverRateLimited_onlyTheTwoWriteEndpointsAre() throws Exception {
        when(roomServiceOrderingService.getById("guest@example.com", "order-1")).thenReturn(view());
        String token = tokenFor("guest-get-many");

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/guest/orders/order-1").header("Authorization", token)).andExpect(status().isOk());
        }
    }

    /**
     * Its own guest id, not shared with any other test in this class - the limiter's state is a
     * singleton bean reused across the whole (context-cached) test class, so a shared id here
     * would make this test's pass/fail depend on how many write calls earlier tests happened to
     * make against that same id first.
     */
    @Test
    void submit_pastTheRateLimit_isTooManyRequests() throws Exception {
        when(roomServiceOrderingService.submit(anyString(), anyString(), any())).thenReturn(view());
        String token = tokenFor("guest-rate-submit");
        String body = "{\"bookingId\":\"b1\",\"items\":[{\"menuItemId\":\"m1\",\"quantity\":1}]}";

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/guest/orders").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/guest/orders").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    /** Same rate limiter bean as {@code submit} (keyed on guest account id, not per-endpoint) - its own guest id, same reasoning as that test. */
    @Test
    void addItems_pastTheRateLimit_isTooManyRequests() throws Exception {
        when(roomServiceOrderingService.addItems(anyString(), anyString(), any())).thenReturn(view());
        String token = tokenFor("guest-rate-additems");
        String body = "[{\"menuItemId\":\"m1\",\"quantity\":1}]";

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/guest/orders/order-1/items").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/guest/orders/order-1/items").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }
}
