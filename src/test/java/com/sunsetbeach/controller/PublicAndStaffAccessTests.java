package com.sunsetbeach.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.security.NextAuthTokenService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.TestSessionTokens;
import com.sunsetbeach.service.AvailabilityService;
import com.sunsetbeach.service.PricingService;
import com.sunsetbeach.service.RoomImageService;
import com.sunsetbeach.service.RoomService;
import com.sunsetbeach.service.UserService;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the auth boundary between staff-only reads, ADMIN-only reads, and public/unauthenticated
 * reads (including real file serving from {@code app.uploads.root}) added alongside the new
 * GET endpoints. Slices in RoomController/UserController/PublicController plus the real security
 * chain (SecurityConfig, NextAuthTokenService) so 401/403/200 boundaries are exercised for real;
 * only the service layer below the controllers is mocked (RoomImageService is real, backed by
 * a temp directory, so the file-serving 200/404 cases hit an actual filesystem lookup).
 */
@WebMvcTest(controllers = {RoomController.class, UserController.class, PublicController.class})
@Import({SecurityConfig.class, NextAuthTokenService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class, RoomImageService.class})
class PublicAndStaffAccessTests {

    private static final String NEXTAUTH_SECRET = "test-nextauth-secret-at-least-32-bytes-long!!";
    private static final String SESSION_COOKIE = "next-auth.session-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private AvailabilityService availabilityService;

    @TempDir
    static Path uploadsRoot;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.nextauth-secret", () -> NEXTAUTH_SECRET);
        registry.add("app.uploads.root", () -> uploadsRoot.toString());
    }

    @BeforeEach
    void setUpUploadedFile() throws IOException {
        Path roomDir = uploadsRoot.resolve("rooms").resolve("room-1");
        Files.createDirectories(roomDir);
        Files.writeString(roomDir.resolve("photo.jpg"), "fake-jpeg-bytes");
    }

    private static Cookie managerCookie() {
        return new Cookie(SESSION_COOKIE, TestSessionTokens.encode(NEXTAUTH_SECRET, "user-1", "manager@example.com", Role.MANAGER));
    }

    @Test
    void publicRoomsList_withoutCookie_isOk() throws Exception {
        when(roomService.list()).thenReturn(List.of(sampleRoom()));
        mockMvc.perform(get("/public/rooms")).andExpect(status().isOk());
    }

    @Test
    void staffRoomsList_withoutCookie_isUnauthorized() throws Exception {
        mockMvc.perform(get("/rooms")).andExpect(status().isUnauthorized());
    }

    @Test
    void staffRoomsList_withManagerCookie_isOk() throws Exception {
        when(roomService.list()).thenReturn(List.of(sampleRoom()));
        mockMvc.perform(get("/rooms").cookie(managerCookie())).andExpect(status().isOk());
    }

    @Test
    void usersList_withManagerCookie_isForbidden() throws Exception {
        mockMvc.perform(get("/users").cookie(managerCookie())).andExpect(status().isForbidden());
    }

    @Test
    void roomImage_existingFile_withoutCookie_isOkWithBytes() throws Exception {
        mockMvc.perform(get("/uploads/rooms/room-1/photo.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("fake-jpeg-bytes".getBytes()));
    }

    @Test
    void roomImage_missingFile_isNotFound() throws Exception {
        mockMvc.perform(get("/uploads/rooms/room-1/does-not-exist.jpg")).andExpect(status().isNotFound());
    }

    // Directory-traversal defense itself (".." in filename/roomId, backslashes, etc.) is
    // covered exhaustively by RoomImageServiceTest at the unit level, where it isn't at the
    // mercy of how MockMvc/the servlet container happen to decode an encoded path segment.

    private static Room sampleRoom() {
        return new Room("room-1", "Ocean View Suite", "A lovely room", 2, "1500.00", List.of(), OffsetDateTime.now());
    }
}
