package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.UserService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real login/register/me boundary: bcrypt verification, JWT issuance, and the
 * ADMIN-only rule on /auth/register, all through the actual SecurityConfig + JwtService.
 */
@WebMvcTest(controllers = {AuthController.class})
@Import({SecurityConfig.class, JwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class, UserMapper.class})
class AuthControllerTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-ttl-days", () -> "7");
    }

    private UserEntity managerEntity() {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setEmail("manager@example.com");
        entity.setPasswordHash(passwordEncoder.encode("correct-password"));
        entity.setRole(Role.MANAGER);
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
        return entity;
    }

    private String managerBearerToken() {
        return "Bearer " + jwtService.issue(new StaffPrincipal("user-1", "manager@example.com", Role.MANAGER));
    }

    @Test
    void login_correctPassword_returnsTokenAndUser() throws Exception {
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(managerEntity()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"manager@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("manager@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void login_wrongPassword_isUnauthorized() throws Exception {
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(managerEntity()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"manager@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void login_unknownEmail_isUnauthorized() throws Exception {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(managerEntity()));

        mockMvc.perform(get("/auth/me").header("Authorization", managerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void me_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void register_withManagerToken_isForbidden() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", managerBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"password1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_withAdminToken_delegatesToUserService() throws Exception {
        String adminToken = "Bearer " + jwtService.issue(new StaffPrincipal("admin-1", "admin@example.com", Role.ADMIN));
        User created = new User("user-2", "new@example.com", Role.MANAGER, OffsetDateTime.now());
        when(userService.create(any(UserCreateInput.class))).thenReturn(created);

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        verify(userService).create(any(UserCreateInput.class));
    }

    @Test
    void logout_isNoContent() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent());
    }
}
