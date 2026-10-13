package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.GymGuestInfo;
import com.sunsetbeach.model.GymHallSettings;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymSchedule;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.GuestJwtService;
import com.sunsetbeach.security.GuestPrincipal;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.GymHallSettingsService;
import com.sunsetbeach.service.GymReservationService;
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
 * Who reaches the gym reservation routes, through the real security chain: the staff schedule is
 * CASHIER and above, the hall settings are ADMIN only (a manager is refused), and the guest routes
 * take a guest token and nothing else - a staff token is refused there and a guest token is refused
 * on the staff routes. Services are mocked; the rules behind the routes are in
 * {@code GymReservationServiceTests}.
 */
@WebMvcTest(controllers = {GymReservationController.class, GuestGymController.class})
@Import({SecurityConfig.class, com.sunsetbeach.config.JacksonConfig.class, JwtService.class, GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
class GymReservationAccessTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private GuestJwtService guestJwtService;

    @MockitoBean private GymReservationService reservations;
    @MockitoBean private GymHallSettingsService hallSettings;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private GuestAccountRepository guestAccountRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> "test-jwt-secret-at-least-32-bytes-long!!");
        registry.add("app.security.jwt-ttl-days", () -> "7");
        registry.add("app.security.guest-jwt-secret", () -> "test-guest-jwt-secret-at-least-32-bytes!!");
        registry.add("app.security.guest-jwt-ttl-days", () -> "30");
    }

    @BeforeEach
    void setUp() {
        for (Role role : List.of(Role.WAITER, Role.CASHIER, Role.MANAGER, Role.ADMIN)) {
            UserEntity user = new UserEntity();
            user.setId(role.getValue() + "-1");
            user.setEmail(role.getValue() + "-1@example.com");
            user.setName(user.getEmail());
            user.setRole(role);
            user.setActive(true);
            user.setTokenVersion(0);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        }
        GuestAccountEntity account = new GuestAccountEntity();
        account.setId("guest-1");
        account.setEmail("guest@example.com");
        account.setEmailVerifiedAt(LocalDateTime.of(2031, 1, 1, 0, 0));
        account.setTokenVersion(0);
        ReflectionTestUtils.setField(account, "createdAt", LocalDateTime.of(2031, 1, 1, 0, 0));
        when(guestAccountRepository.findById("guest-1")).thenReturn(Optional.of(account));
        when(reservations.schedule(anyString(), anyString(), anyBoolean()))
                .thenReturn(new GymSchedule("2031-06-15", "2031-06-15", "06:00", "22:00", 30, 30, 120, 120, List.of()));
        when(hallSettings.get()).thenReturn(new GymHallSettings("06:00", "22:00", 30, 0, 16, 3));
        when(reservations.guestInfo("guest-1"))
                .thenReturn(new GymGuestInfo(false, List.of(), "06:00", "22:00", 30, 30, 120, 120).message("Check in first"));
    }

    @Test
    void theStaffSchedule_isCashierAndAbove() throws Exception {
        String url = "/gym/reservations?from=2031-06-15&to=2031-06-15";
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(url).header("Authorization", staff(Role.WAITER))).andExpect(status().isForbidden());
        for (Role role : List.of(Role.CASHIER, Role.MANAGER, Role.ADMIN)) {
            mockMvc.perform(get(url).header("Authorization", staff(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.openTime").value("06:00"));
        }
    }

    @Test
    void creatingMovingAndCancelling_areRefusedForAWaiter_andForAGuestToken() throws Exception {
        String create = "{\"bookingId\":\"b1\",\"date\":\"2031-06-15\",\"startTime\":\"12:00\",\"durationMinutes\":60}";
        String move = "{\"date\":\"2031-06-15\",\"startTime\":\"12:00\",\"durationMinutes\":60}";
        for (String auth : List.of(staff(Role.WAITER), guest())) {
            mockMvc.perform(post("/gym/reservations").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(create))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/gym/reservations/r1/schedule").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(move))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/gym/reservations/r1/cancel").header("Authorization", auth)).andExpect(status().isForbidden());
        }
        verify(reservations, never()).createForStaff(any(), anyString());
        verify(reservations, never()).cancelByStaff(anyString(), any(), anyString());
    }

    @Test
    void theHallSettings_areAdminOnly_evenForManagers() throws Exception {
        String body = "{\"openTime\":\"06:00\",\"closeTime\":\"22:00\",\"leadMinutes\":30,\"acMode\":0,\"acTemp\":16,\"acWind\":3}";
        for (Role role : List.of(Role.WAITER, Role.CASHIER, Role.MANAGER)) {
            mockMvc.perform(get("/gym/settings/hall").header("Authorization", staff(role))).andExpect(status().isForbidden());
            mockMvc.perform(put("/gym/settings/hall").header("Authorization", staff(role)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/gym/settings/hall").header("Authorization", staff(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadMinutes").value(30));
    }

    @Test
    void narrowingTheHours_overReservations_savesNothingUntilForced() throws Exception {
        String body = "{\"openTime\":\"08:00\",\"closeTime\":\"20:00\",\"leadMinutes\":30,\"acMode\":0,\"acTemp\":16,\"acWind\":3}";
        GymReservation outside = new GymReservation("r1", "b1", "Guest", "2031-06-16", "06:00", "07:00", 60,
                com.sunsetbeach.model.GymReservationStatus.BOOKED, GymReservation.CreatedByTypeEnum.STAFF, "Desk", new com.sunsetbeach.model.GymAutomation(),
                java.time.OffsetDateTime.parse("2031-06-14T00:00:00Z"));
        when(hallSettings.validatedHours(any())).thenReturn(new java.time.LocalTime[] {java.time.LocalTime.of(8, 0), java.time.LocalTime.of(20, 0)});
        when(reservations.outsideHours(any(), any())).thenReturn(List.of(outside));
        when(hallSettings.update(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(new GymHallSettings("08:00", "20:00", 30, 0, 16, 3));

        mockMvc.perform(put("/gym/settings/hall").header("Authorization", staff(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false))
                .andExpect(jsonPath("$.outsideHours[0].id").value("r1"))
                .andExpect(jsonPath("$.settings.openTime").value("06:00"));
        verify(hallSettings, never()).update(any(), org.mockito.ArgumentMatchers.anyInt());

        mockMvc.perform(put("/gym/settings/hall?force=true").header("Authorization", staff(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.settings.openTime").value("08:00"))
                .andExpect(jsonPath("$.outsideHours[0].id").value("r1"));
        verify(hallSettings).update(any(), org.mockito.ArgumentMatchers.eq(1));
    }

    @Test
    void theAirConditionerValues_outsideTheirRanges_areA400() throws Exception {
        for (String body : List.of(
                "{\"openTime\":\"06:00\",\"closeTime\":\"22:00\",\"leadMinutes\":30,\"acMode\":0,\"acTemp\":15,\"acWind\":3}",
                "{\"openTime\":\"06:00\",\"closeTime\":\"22:00\",\"leadMinutes\":30,\"acMode\":5,\"acTemp\":16,\"acWind\":3}",
                "{\"openTime\":\"06:00\",\"closeTime\":\"22:00\",\"leadMinutes\":30,\"acMode\":0,\"acTemp\":16,\"acWind\":4}",
                "{\"openTime\":\"06:00\",\"closeTime\":\"22:00\",\"leadMinutes\":500,\"acMode\":0,\"acTemp\":16,\"acWind\":3}")) {
            mockMvc.perform(put("/gym/settings/hall").header("Authorization", staff(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void theGuestRoutes_takeAGuestTokenOnly() throws Exception {
        mockMvc.perform(get("/guest/gym/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/guest/gym/info").header("Authorization", staff(Role.ADMIN))).andExpect(status().isForbidden());
        mockMvc.perform(get("/guest/gym/info").header("Authorization", guest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.message").value("Check in first"));
    }

    @Test
    void aGuestWhoIsNotCheckedIn_getsA403WithAPlainMessage() throws Exception {
        when(reservations.createForGuest(anyString(), any())).thenThrow(new ForbiddenException("The gym can be booked once you have checked in at the front desk."));

        mockMvc.perform(post("/guest/gym/reservations").header("Authorization", guest()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2031-06-16\",\"startTime\":\"12:00\",\"durationMinutes\":60}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("The gym can be booked once you have checked in at the front desk."));
    }

    private String staff(Role role) {
        return "Bearer " + jwtService.issue(new StaffPrincipal(role.getValue() + "-1", role.getValue() + "-1@example.com", role), 0);
    }

    private String guest() {
        return "Bearer " + guestJwtService.issue(new GuestPrincipal("guest-1", "guest@example.com"), 0);
    }
}
