package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SiteMinderImportAction;
import com.sunsetbeach.model.SiteMinderImportResult;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.SiteMinderImportService;
import com.sunsetbeach.service.SiteMinderRoomTypeMappingService;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * The SiteMinder integration's auth boundary, through the real security chain: the integration
 * key opens the import route and nothing else, a staff token can't use the import route, and the
 * mapping table is MANAGER+. Services are mocked - the import's behavior is
 * {@code SiteMinderImportServiceTests}' job.
 */
@WebMvcTest(controllers = SiteMinderController.class)
@Import({SecurityConfig.class, JwtService.class, com.sunsetbeach.security.GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
class SiteMinderAccessTests {

    private static final String KEY = "test-siteminder-integration-key";
    private static final String IMPORT = "/integrations/siteminder/reservations";
    private static final String MAPPINGS = "/integrations/siteminder/room-type-mappings";
    private static final String BODY = """
            {"reference":"SM-1","status":"BOOKED","firstName":"Jane","lastName":"Traveller","checkIn":"2035-03-10",
             "checkOut":"2035-03-13","roomTypeName":"Garden Jacuzzi Villa ABF","adults":2,"totalPrice":"9000.00",
             "channel":"Booking.com","bookedAt":"2026-09-01T10:00:00+07:00"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private SiteMinderImportService importService;
    @MockitoBean private SiteMinderRoomTypeMappingService mappingService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private com.sunsetbeach.repository.GuestAccountRepository guestAccountRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> "test-jwt-secret-at-least-32-bytes-long!!");
        registry.add("app.security.jwt-ttl-days", () -> "7");
        registry.add("app.security.guest-jwt-secret", () -> "test-guest-jwt-secret-at-least-32-bytes!!");
        registry.add("app.security.guest-jwt-ttl-days", () -> "30");
        registry.add("app.integrations.siteminder.api-key", () -> KEY);
    }

    @BeforeEach
    void setUp() {
        for (String id : List.of("manager-1", "cashier-1")) {
            UserEntity user = new UserEntity();
            user.setId(id);
            user.setEmail(id + "@example.com");
            user.setName(user.getEmail());
            user.setActive(true);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
        }
        when(importService.importReservation(any()))
                .thenReturn(new SiteMinderImportResult(SiteMinderImportAction.CREATED, "booking-1", List.of(), List.of(), null));
        when(mappingService.list()).thenReturn(List.of());
    }

    private String bearer(String id, Role role) {
        return "Bearer " + jwtService.issue(new StaffPrincipal(id, id + "@example.com", role));
    }

    @Test
    void import_withTheKey_isOk() throws Exception {
        mockMvc.perform(post(IMPORT).header("X-Integration-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("CREATED"));
    }

    @Test
    void import_withoutAKey_orWithTheWrongOne_isUnauthorized() throws Exception {
        mockMvc.perform(post(IMPORT).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(IMPORT).header("X-Integration-Key", KEY + "x").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verify(importService, never()).importReservation(any());
    }

    @Test
    void import_withAStaffToken_isForbidden_evenForAManager() throws Exception {
        mockMvc.perform(post(IMPORT).header("Authorization", bearer("manager-1", Role.MANAGER)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(importService, never()).importReservation(any());
    }

    @Test
    void theKey_authenticatesNothingButTheImportRoute() throws Exception {
        mockMvc.perform(get(MAPPINGS).header("X-Integration-Key", KEY)).andExpect(status().isUnauthorized());
        // A route whose rule is just authenticated() - the key must not satisfy it either.
        mockMvc.perform(get("/auth/me").header("X-Integration-Key", KEY)).andExpect(status().isUnauthorized());
    }

    @Test
    void mappings_areManagerPlus() throws Exception {
        mockMvc.perform(get(MAPPINGS).header("Authorization", bearer("manager-1", Role.MANAGER))).andExpect(status().isOk());
        mockMvc.perform(get(MAPPINGS).header("Authorization", bearer("cashier-1", Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void unmappedRoomType_isA400NamingTheField() throws Exception {
        when(importService.importReservation(any())).thenThrow(ValidationException.field(
                "roomTypeName", "SiteMinder room type \"Garden Jacuzzi Villa ABF\" isn't mapped to a room type"));

        mockMvc.perform(post(IMPORT).header("X-Integration-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.roomTypeName[0]").value(org.hamcrest.Matchers.containsString("Garden Jacuzzi Villa ABF")));
    }
}
