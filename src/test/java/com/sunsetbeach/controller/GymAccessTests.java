package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.GymDevice;
import com.sunsetbeach.model.GymDeviceList;
import com.sunsetbeach.model.GymDeviceType;
import com.sunsetbeach.model.GymTuyaUsage;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.GymDeviceService;
import com.sunsetbeach.service.GymSettingsService;
import com.sunsetbeach.tuya.TuyaException;
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
 * Who may do what on the gym routes, through the real security chain: switching a fan or an air
 * conditioner is CASHIER and above (a waiter is refused), the device list, the Tuya project listing
 * and the call counter are ADMIN only, and a Tuya failure reaches the client as a plain message
 * with 503 (integration down) or 502 (one request refused). Services are mocked.
 */
@WebMvcTest(controllers = GymController.class)
@Import({SecurityConfig.class, JwtService.class, com.sunsetbeach.security.GuestJwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
class GymAccessTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private GymDeviceService gymDeviceService;
    @MockitoBean private GymSettingsService gymSettingsService;
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
        when(gymDeviceService.list(anyBoolean())).thenReturn(new GymDeviceList(List.of(), OffsetDateTime.now()));
        when(gymDeviceService.setRelay(anyString(), anyBoolean()))
                .thenReturn(new GymDevice("d1", "roof fan 1", GymDeviceType.RELAY, 0));
        when(gymSettingsService.usage()).thenReturn(new GymTuyaUsage("2026-10", 12L, 26000));
    }

    @Test
    void waiter_cannotSeeOrSwitchAnything() throws Exception {
        mockMvc.perform(get("/gym/devices").header("Authorization", bearer(Role.WAITER))).andExpect(status().isForbidden());
        mockMvc.perform(post("/gym/devices/d1/relay").header("Authorization", bearer(Role.WAITER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true}"))
                .andExpect(status().isForbidden());
        verify(gymDeviceService, never()).setRelay(anyString(), anyBoolean());
    }

    @Test
    void noToken_is401() throws Exception {
        mockMvc.perform(get("/gym/devices")).andExpect(status().isUnauthorized());
    }

    @Test
    void cashier_mayReadAndSwitch() throws Exception {
        mockMvc.perform(get("/gym/devices?refresh=true").header("Authorization", bearer(Role.CASHIER))).andExpect(status().isOk());
        verify(gymDeviceService).list(true);
        mockMvc.perform(post("/gym/devices/d1/relay").header("Authorization", bearer(Role.CASHIER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("roof fan 1"));
        verify(gymDeviceService).setRelay("d1", true);
        mockMvc.perform(post("/gym/devices/d1/ac").header("Authorization", bearer(Role.CASHIER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"power\":1,\"mode\":0,\"temp\":24,\"wind\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void acCommand_outsideTheAllowedRanges_isA400_andNeverReachesTheService() throws Exception {
        for (String body : List.of(
                "{\"power\":2,\"mode\":0,\"temp\":24,\"wind\":1}",
                "{\"power\":1,\"mode\":5,\"temp\":24,\"wind\":1}",
                "{\"power\":1,\"mode\":0,\"temp\":15,\"wind\":1}",
                "{\"power\":1,\"mode\":0,\"temp\":31,\"wind\":1}",
                "{\"power\":1,\"mode\":0,\"temp\":24,\"wind\":4}",
                "{\"power\":1,\"mode\":0,\"temp\":24}")) {
            mockMvc.perform(post("/gym/devices/d1/ac").header("Authorization", bearer(Role.CASHIER)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(gymDeviceService, never()).sendAcCommand(anyString(), any());
    }

    @Test
    void settingsAndUsage_areAdminOnly_evenForManagers() throws Exception {
        for (Role role : List.of(Role.WAITER, Role.CASHIER, Role.MANAGER)) {
            String auth = bearer(role);
            mockMvc.perform(get("/gym/settings/devices").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(get("/gym/settings/usage").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(get("/gym/settings/tuya/devices").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(get("/gym/settings/tuya/devices/x/switch-codes").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(get("/gym/settings/tuya/infrareds/x/remotes").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(post("/gym/settings/devices").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\",\"type\":\"RELAY\",\"tuyaDeviceId\":\"x\",\"switchCode\":\"switch_1\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/gym/settings/devices/d1").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\",\"type\":\"RELAY\",\"tuyaDeviceId\":\"x\",\"switchCode\":\"switch_1\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/gym/settings/devices/d1").header("Authorization", auth)).andExpect(status().isForbidden());
            mockMvc.perform(post("/gym/settings/devices/reorder").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ids\":[]}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/gym/settings/usage").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calls").value(12))
                .andExpect(jsonPath("$.month").value("2026-10"));
    }

    @Test
    void integrationDown_is503_withTheOwnersText() throws Exception {
        String text = "Tuya integration is unavailable — check the IoT Core subscription (extend the trial period at iot.tuya.com)";
        when(gymDeviceService.setRelay(anyString(), anyBoolean())).thenThrow(new TuyaException(TuyaException.Kind.UNAVAILABLE, text));

        mockMvc.perform(post("/gym/devices/d1/relay").header("Authorization", bearer(Role.CASHIER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value(text));
    }

    @Test
    void oneRefusedRequest_is502() throws Exception {
        when(gymDeviceService.setRelay(anyString(), anyBoolean()))
                .thenThrow(new TuyaException(TuyaException.Kind.REJECTED, "Tuya rejected the request. Details are in the server log"));

        mockMvc.perform(post("/gym/devices/d1/relay").header("Authorization", bearer(Role.CASHIER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOn\":true}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Tuya rejected the request. Details are in the server log"));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new StaffPrincipal(role.getValue() + "-1", role.getValue() + "-1@example.com", role), 0);
    }
}
