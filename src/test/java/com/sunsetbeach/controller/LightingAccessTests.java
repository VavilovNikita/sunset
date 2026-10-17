package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.lighting.WledException;
import com.sunsetbeach.model.LightingDevice;
import com.sunsetbeach.model.LightingDeviceList;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.LightingService;
import com.sunsetbeach.service.LightingSettingsService;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Who may do what on the lighting routes, through the real security chain: reading the live state and
 * sending commands is MANAGER and above (SecurityConfig.LIGHTING_CONTROL_ROLE), everything under
 * /lighting/settings - including the address test that makes the server send a request - is ADMIN
 * only, and an unreachable controller is a 502 carrying its own message. Services are mocked.
 */
@WebMvcTest(controllers = LightingController.class)
@Import({SecurityConfig.class, JwtService.class, com.sunsetbeach.security.GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
class LightingAccessTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private LightingService lightingService;
    @MockitoBean private LightingSettingsService lightingSettingsService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private com.sunsetbeach.repository.GuestAccountRepository guestAccountRepository;

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
        when(lightingService.list(anyBoolean())).thenReturn(new LightingDeviceList(List.of(), OffsetDateTime.now()));
        when(lightingService.setState(anyString(), any())).thenReturn(new LightingDevice("d1", "Pool bar", true, List.of()));
    }

    @Test
    void waiterAndCashier_cannotSeeOrControlAnything() throws Exception {
        for (Role role : List.of(Role.WAITER, Role.CASHIER)) {
            mockMvc.perform(get("/lighting/devices").header("Authorization", bearer(role))).andExpect(status().isForbidden());
            mockMvc.perform(post("/lighting/devices/d1/state").header("Authorization", bearer(role)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isOn\":true}"))
                    .andExpect(status().isForbidden());
        }
        verify(lightingService, never()).setState(anyString(), any());
    }

    @Test
    void noToken_is401() throws Exception {
        mockMvc.perform(get("/lighting/devices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/lighting/settings/devices")).andExpect(status().isUnauthorized());
    }

    @Test
    void manager_mayReadAndControl() throws Exception {
        mockMvc.perform(get("/lighting/devices?refresh=true").header("Authorization", bearer(Role.MANAGER))).andExpect(status().isOk());
        verify(lightingService).list(true);
        mockMvc.perform(post("/lighting/devices/d1/state").header("Authorization", bearer(Role.MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true,\"brightness\":128,\"presetId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pool bar"));
    }

    @Test
    void stateValues_outsideTheAllowedRanges_areA400_andNeverReachTheService() throws Exception {
        for (String body : List.of("{\"brightness\":256}", "{\"brightness\":-1}", "{\"presetId\":0}", "{\"presetId\":251}")) {
            mockMvc.perform(post("/lighting/devices/d1/state").header("Authorization", bearer(Role.MANAGER)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(lightingService, never()).setState(anyString(), any());
    }

    @Test
    void settings_areAdminOnly_evenForManagers() throws Exception {
        for (Role role : List.of(Role.WAITER, Role.CASHIER, Role.MANAGER)) {
            String auth = bearer(role);
            mockMvc.perform(get("/lighting/settings/devices").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(post("/lighting/settings/devices").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\",\"host\":\"192.168.1.5\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/lighting/settings/devices/check").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"host\":\"192.168.1.5\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/lighting/settings/devices/reorder").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[]}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/lighting/settings/devices/d1").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":false}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/lighting/settings/devices/d1").header("Authorization", auth)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/lighting/settings/devices").header("Authorization", bearer(Role.ADMIN))).andExpect(status().isOk());
    }

    @Test
    void anUnreachableController_is502_withItsOwnMessage() throws Exception {
        when(lightingService.setState(anyString(), any()))
                .thenThrow(new WledException(WledException.Kind.UNREACHABLE, "Controller \"Pool bar\" did not answer in time"));

        mockMvc.perform(post("/lighting/devices/d1/state").header("Authorization", bearer(Role.MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Controller \"Pool bar\" did not answer in time"));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new StaffPrincipal(role.getValue() + "-1", role.getValue() + "-1@example.com", role), 0);
    }
}
