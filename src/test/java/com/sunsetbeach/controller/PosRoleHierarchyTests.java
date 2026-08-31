package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.config.JacksonConfig;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.model.AvailabilityResponse;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingSegment;
import com.sunsetbeach.model.BookingCalendarResponse;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.model.PricingResponse;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJob;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.AvailabilityService;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.service.MenuService;
import com.sunsetbeach.service.OrderService;
import com.sunsetbeach.service.PaymentService;
import com.sunsetbeach.service.PricingService;
import com.sunsetbeach.service.PrinterService;
import com.sunsetbeach.service.RoomService;
import com.sunsetbeach.service.RoomUnitService;
import com.sunsetbeach.service.ShiftService;
import com.sunsetbeach.service.UserService;
import java.math.BigDecimal;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the RoleHierarchy bean (ADMIN &gt; MANAGER &gt; CASHIER &gt; WAITER) behaves as
 * intended for the POS module's per-path role checks, and - the risk item this exists to close
 * - that it does NOT loosen the hard ADMIN-only restriction on {@code /users/**}.
 */
@WebMvcTest(
        controllers = {
            MenuController.class,
            OrderController.class,
            UserController.class,
            ShiftController.class,
            PaymentController.class,
            PrinterController.class,
            RoomUnitController.class,
            BookingController.class,
            RoomController.class,
            PricingController.class,
            AvailabilityController.class,
            com.sunsetbeach.controller.AuditLogController.class
        })
@Import({SecurityConfig.class, JwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class, JacksonConfig.class,
        com.sunsetbeach.security.BookingRateLimiter.class})
class PosRoleHierarchyTests {

    private static final String JWT_SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PrinterService printerService;

    @MockitoBean
    private RoomUnitService roomUnitService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private com.sunsetbeach.service.BookingCalendarService bookingCalendarService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private com.sunsetbeach.service.AuditLogService auditLogService;

    // JwtAuthFilter now re-checks the issuing user's active/tokenVersion against the DB on every
    // request (see JwtAuthFilter/JwtService.ParsedToken) - every token this class issues uses id
    // "user-1" regardless of role, so one stub covers every test.
    @MockitoBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-ttl-days", () -> "7");
    }

    @BeforeEach
    void stubActiveUser() {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setEmail("user-1@example.com");
        entity.setActive(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(entity));
    }

    private String token(Role role) {
        return "Bearer " + jwtService.issue(new StaffPrincipal("user-1", role.getValue().toLowerCase() + "@example.com", role));
    }

    // --- /users/** stays ADMIN-only despite the hierarchy granting everything else down-chain ---

    @Test
    void usersList_withAdminToken_isOk() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", token(Role.ADMIN))).andExpect(status().isOk());
    }

    @Test
    void usersList_withManagerToken_isForbidden() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", token(Role.MANAGER))).andExpect(status().isForbidden());
    }

    @Test
    void usersList_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void usersList_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void resetUserPassword_withManagerToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/users/user-2/password")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"a-new-password1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetUserPassword_withAdminToken_isOk() throws Exception {
        when(userService.resetPassword(eq("user-2"), anyString())).thenReturn(sampleUser());
        mockMvc.perform(patch("/users/user-2/password")
                        .header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"a-new-password1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserActive_withManagerToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/users/user-2/active")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserActive_withAdminToken_isOk() throws Exception {
        when(userService.setActive(eq("user-2"), anyString(), eq(false))).thenReturn(sampleUser());
        mockMvc.perform(patch("/users/user-2/active")
                        .header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
    }

    // --- POST /menu requires MANAGER or above ---

    @Test
    void createMenuItem_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/menu")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMenuItemInput())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMenuItem_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(post("/menu")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMenuItemInput())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMenuItem_withManagerToken_isCreated() throws Exception {
        when(menuService.create(any())).thenReturn(sampleMenuItem());
        mockMvc.perform(post("/menu")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMenuItemInput())))
                .andExpect(status().isCreated());
    }

    @Test
    void createMenuItem_withAdminToken_isCreated() throws Exception {
        // Hierarchy: ADMIN > MANAGER, so ADMIN must also satisfy a hasRole(MANAGER) check.
        when(menuService.create(any())).thenReturn(sampleMenuItem());
        mockMvc.perform(post("/menu")
                        .header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleMenuItemInput())))
                .andExpect(status().isCreated());
    }

    // --- POST /orders requires WAITER or above (i.e. any staff role) ---

    @Test
    void createOrder_withWaiterToken_isCreated() throws Exception {
        when(orderService.create(any(), anyString())).thenReturn(sampleOrder());
        mockMvc.perform(post("/orders")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderCreateInput())))
                .andExpect(status().isCreated());
    }

    // --- POST /orders/{id}/close requires CASHIER or above - WAITER is not enough ---

    @Test
    void closeOrder_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/orders/order-1/close")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseOrderInput(PaymentMethod.CASH))))
                .andExpect(status().isForbidden());
    }

    @Test
    void closeOrder_withCashierToken_isOk() throws Exception {
        when(orderService.close(anyString(), any(), anyString())).thenReturn(sampleOrder());
        mockMvc.perform(post("/orders/order-1/close")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseOrderInput(PaymentMethod.CASH))))
                .andExpect(status().isOk());
    }

    // --- GET /shifts/{id}/export requires MANAGER or above - CASHIER is not enough ---

    @Test
    void exportShift_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/shifts/shift-1/export").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void exportShift_withManagerToken_isOk() throws Exception {
        when(shiftService.exportCsv(anyString())).thenReturn("ID,Order,Method,Amount,Booking,Recorded by,Created at");
        mockMvc.perform(get("/shifts/shift-1/export").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    // --- GET /payments/summary requires MANAGER or above - CASHIER is not enough ---

    @Test
    void paymentsSummary_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/payments/summary?from=2031-01-01&to=2031-01-31").header("Authorization", token(Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentsSummary_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/payments/summary?from=2031-01-01&to=2031-01-31").header("Authorization", token(Role.WAITER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentsSummary_withManagerToken_isOk() throws Exception {
        when(paymentService.getSummary(anyString(), anyString())).thenReturn(samplePaymentsSummary());
        mockMvc.perform(get("/payments/summary?from=2031-01-01&to=2031-01-31").header("Authorization", token(Role.MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    void paymentsSummary_missingFromParam_isBadRequest() throws Exception {
        mockMvc.perform(get("/payments/summary?to=2031-01-31").header("Authorization", token(Role.MANAGER))).andExpect(status().isBadRequest());
    }

    // --- GET /audit-log requires MANAGER or above - same reasoning as /payments/summary above:
    // the disputes it exists to resolve need to be investigable without escalating to an admin. ---

    @Test
    void auditLog_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/audit-log").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void auditLog_withManagerToken_isOk() throws Exception {
        when(auditLogService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new com.sunsetbeach.model.AuditLogPage(List.of(), 0, 50, 0));
        mockMvc.perform(get("/audit-log").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    // --- /printers and /print-jobs require MANAGER or above ---

    @Test
    void listPrinters_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/printers").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void listPrinters_withManagerToken_isOk() throws Exception {
        when(printerService.list()).thenReturn(List.of());
        mockMvc.perform(get("/printers").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    @Test
    void listPrintJobs_withWaiterToken_isOk() throws Exception {
        // Unlike /printers/**, the queue itself is WAITER+ - PrinterService does the actual
        // per-role document-type filtering, not the security gate.
        when(printerService.listPrintJobs(any(), any(), eq(Role.WAITER))).thenReturn(List.of());
        mockMvc.perform(get("/print-jobs").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void listPrintJobs_withManagerToken_isOk() throws Exception {
        when(printerService.listPrintJobs(any(), any(), eq(Role.MANAGER))).thenReturn(List.of());
        mockMvc.perform(get("/print-jobs").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    @Test
    void previewPrintJob_withWaiterToken_isOk() throws Exception {
        when(printerService.previewPrintJob(anyString(), eq(Role.WAITER))).thenReturn("KITCHEN TICKET\nTable 4");
        mockMvc.perform(get("/print-jobs/job-1/preview").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void retryPrintJob_withWaiterToken_isOk() throws Exception {
        when(printerService.retryPrintJob(anyString(), eq(Role.WAITER))).thenReturn(samplePrintJob());
        mockMvc.perform(post("/print-jobs/job-1/retry").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    // --- GET /room-units and GET /room-units/{id} are open to any staff role, including WAITER
    // - room numbers aren't sensitive, and a CASHIER needs this list to pick a candidate before
    // calling PUT /bookings/{id}/room-unit below. Mutating endpoints stay MANAGER-only. ---

    @Test
    void listRoomUnits_withWaiterToken_isOk() throws Exception {
        when(roomUnitService.list(null)).thenReturn(List.of());
        mockMvc.perform(get("/room-units").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void getRoomUnit_withWaiterToken_isOk() throws Exception {
        when(roomUnitService.getById("unit-1")).thenReturn(sampleRoomUnit());
        mockMvc.perform(get("/room-units/unit-1").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void createRoomUnit_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(post("/room-units")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitInput("room-1", "203"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoomUnit_withManagerToken_isCreated() throws Exception {
        when(roomUnitService.create(any())).thenReturn(sampleRoomUnit());
        mockMvc.perform(post("/room-units")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitInput("room-1", "203"))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateRoomUnit_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/room-units/unit-1")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitUpdateInput("203", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRoomUnit_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(delete("/room-units/unit-1").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void deleteRoomUnit_withManagerToken_isOk() throws Exception {
        mockMvc.perform(delete("/room-units/unit-1").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    // --- Manual blocks (RoomUnitBlock.reason can carry internal staff notes) stay MANAGER-only
    // even though the parent GET /room-units above is open to every staff role - the two must
    // not be assumed to share visibility. ---

    @Test
    void listRoomUnitBlocks_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/room-units/unit-1/blocks").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void listRoomUnitBlocks_withManagerToken_isOk() throws Exception {
        when(roomUnitService.listBlocks("unit-1")).thenReturn(List.of());
        mockMvc.perform(get("/room-units/unit-1/blocks").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    // --- PUT /bookings/{id}/room-unit requires CASHIER or above - and now that GET /room-units
    // is WAITER+, a CASHIER can actually list candidates before calling it. This is the
    // asymmetry (action allowed, prerequisite read blocked) this test class was missing
    // coverage for. ---

    @Test
    void assignBookingRoomUnit_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(put("/bookings/booking-1/room-unit")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitAssignmentInput().roomUnitId("unit-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignBookingRoomUnit_withCashierToken_isOk() throws Exception {
        when(bookingService.assignRoomUnit(eq("booking-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(put("/bookings/booking-1/room-unit")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitAssignmentInput().roomUnitId("unit-1"))))
                .andExpect(status().isOk());
    }

    /**
     * Regression test for a real bug this endpoint had: {@code roomUnitId} used to be listed
     * under {@code required} in openapi.yaml (nullable but required-present), which made the
     * generator emit {@code @NotNull} on the {@code JsonNullable<String>} getter. Because
     * {@code org.openapitools:jackson-databind-nullable}'s Jakarta integration registers an
     * {@code @UnwrapByDefault ValueExtractor} for {@code JsonNullable}, that {@code @NotNull}
     * validated the *unwrapped* value - silently rejecting the exact {@code roomUnitId: null}
     * payload this endpoint exists to accept, with a 400 instead of clearing the assignment. No
     * prior test sent an explicit JSON {@code null} through the real {@code @Valid} pipeline
     * (existing coverage only exercised non-null assignment), so this went uncaught.
     */
    @Test
    void assignBookingRoomUnit_withExplicitNull_unassigns() throws Exception {
        when(bookingService.assignRoomUnit(eq("booking-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(put("/bookings/booking-1/room-unit")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomUnitId\":null}"))
                .andExpect(status().isOk());
    }

    // --- Access for /rooms/**, /pricing/**, GET /bookings*, GET /availability/{roomId} was
    // brought in line with what openapi.yaml documents (previously all of these silently fell
    // through to anyRequest().authenticated(), open to any staff role including WAITER
    // regardless of the documented role). See EndpointCoverageTests for the structural guard
    // that catches a future path missing a rule like this entirely; these confirm the specific
    // roles landed correctly for the paths that changed. ---

    @Test
    void listRooms_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/rooms").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    // CASHIER+ (not just MANAGER+): a CASHIER quoting a room type to a walk-in via
    // POST /bookings/staff needs to read this list through an authenticated endpoint - see
    // SecurityConfig's GET /rooms, /rooms/* rule and openapi.yaml's updated description.
    @Test
    void listRooms_withCashierToken_isOk() throws Exception {
        when(roomService.list()).thenReturn(List.of());
        mockMvc.perform(get("/rooms").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void listRooms_withManagerToken_isOk() throws Exception {
        when(roomService.list()).thenReturn(List.of());
        mockMvc.perform(get("/rooms").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    @Test
    void createRoom_withCashierToken_isForbidden() throws Exception {
        // Reads are CASHIER+, writes stay MANAGER-only - same read/write split as RoomUnits.
        mockMvc.perform(post("/rooms")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"description\":\"x\",\"capacity\":1,\"quantity\":1,\"basePrice\":\"100.00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRoomPricing_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/pricing/room-1").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    // CASHIER+ (not just MANAGER+): same reasoning as GET /rooms above - a CASHIER needs to
    // quote a price to a walk-in guest through an authenticated endpoint.
    @Test
    void getRoomPricing_withCashierToken_isOk() throws Exception {
        when(pricingService.getPricing(eq("room-1"), any())).thenReturn(new PricingResponse(BigDecimal.TEN, List.of()));
        mockMvc.perform(get("/pricing/room-1").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void getRoomPricing_withManagerToken_isOk() throws Exception {
        when(pricingService.getPricing(eq("room-1"), any())).thenReturn(new PricingResponse(BigDecimal.TEN, List.of()));
        mockMvc.perform(get("/pricing/room-1").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    @Test
    void setRoomPricing_withCashierToken_isForbidden() throws Exception {
        // PATCH (setting a price override) stays MANAGER-only even though GET is now CASHIER+.
        mockMvc.perform(patch("/pricing/room-1")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2031-01-01\",\"to\":\"2031-01-02\",\"price\":100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRoomAvailability_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/availability/room-1").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void getRoomAvailability_withCashierToken_isOk() throws Exception {
        when(availabilityService.getAvailability(eq("room-1"), any())).thenReturn(new AvailabilityResponse(List.of()));
        mockMvc.perform(get("/availability/room-1").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void listBookings_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/bookings").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void listBookings_withCashierToken_isOk() throws Exception {
        when(bookingService.list(any(), any(), any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/bookings").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void updateBookingStatus_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/bookings/booking-1")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportBookings_withCashierToken_isForbidden() throws Exception {
        // The one deliberate exception in the Bookings group: bulk guest-PII export stays
        // MANAGER+ even though the single-booking reads/writes above are CASHIER+.
        mockMvc.perform(get("/bookings/export").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void exportBookings_withManagerToken_isOk() throws Exception {
        when(bookingService.exportCsv(any(), any(), any())).thenReturn("ID,Room,Guest,Email,Phone,Check-in,Check-out,Total,Status,Payment note,Created at");
        mockMvc.perform(get("/bookings/export").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    // --- POST /bookings/staff, GET /bookings/calendar, PATCH /bookings/{id}/schedule and
    // POST /bookings/{id}/schedule/quote all require CASHIER or above - front-desk operations
    // for the booking calendar grid, same role as the other single-booking Bookings endpoints
    // above. ---

    @Test
    void createStaffBooking_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffBookingCreateInput("room-1", "Guest", "2031-01-01", "2031-01-02"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStaffBooking_withCashierToken_isCreated() throws Exception {
        when(bookingService.createStaffBooking(any())).thenReturn(sampleBooking());
        mockMvc.perform(post("/bookings/staff")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffBookingCreateInput("room-1", "Guest", "2031-01-01", "2031-01-02"))))
                .andExpect(status().isCreated());
    }

    @Test
    void getBookingsCalendar_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/bookings/calendar?from=2031-01-01&to=2031-01-08").header("Authorization", token(Role.WAITER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBookingsCalendar_withCashierToken_isOk() throws Exception {
        when(bookingCalendarService.getCalendar(any(), any())).thenReturn(new BookingCalendarResponse(
                "2031-01-01", "2031-01-08", List.of(), List.of(), List.of()));
        mockMvc.perform(get("/bookings/calendar?from=2031-01-01&to=2031-01-08").header("Authorization", token(Role.CASHIER)))
                .andExpect(status().isOk());
    }

    @Test
    void updateBookingSchedule_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/bookings/booking-1/schedule")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingScheduleInput("2031-01-01", "2031-01-02").roomUnitId(null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBookingSchedule_withCashierToken_isOk() throws Exception {
        when(bookingService.updateSchedule(eq("booking-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(patch("/bookings/booking-1/schedule")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingScheduleInput("2031-01-01", "2031-01-02").roomUnitId(null))))
                .andExpect(status().isOk());
    }

    @Test
    void quoteBookingSchedule_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/bookings/booking-1/schedule/quote")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingScheduleInput("2031-01-01", "2031-01-02").roomUnitId(null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void quoteBookingSchedule_withCashierToken_isOk() throws Exception {
        when(bookingService.quoteSchedule(eq("booking-1"), any()))
                .thenReturn(new BookingScheduleQuote("1500.00", 1, true, null));
        mockMvc.perform(post("/bookings/booking-1/schedule/quote")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingScheduleInput("2031-01-01", "2031-01-02").roomUnitId(null))))
                .andExpect(status().isOk());
    }

    private static RoomUnit sampleRoomUnit() {
        return new RoomUnit("unit-1", "room-1", "203", true, OffsetDateTime.now());
    }

    private static Room sampleRoom() {
        return new Room("room-1", "Ocean View Suite", "A lovely room", 2, 3, "1500.00", List.of(), OffsetDateTime.now());
    }

    private static com.sunsetbeach.model.User sampleUser() {
        return new com.sunsetbeach.model.User("user-2", "user-2@example.com", Role.CASHIER, true, OffsetDateTime.now());
    }

    private static BookingSegment sampleBookingSegment() {
        return new BookingSegment("segment-1", "room-1", sampleRoom(), "unit-1", sampleRoomUnit(), "2026-01-01", "2026-01-02", "1500.00");
    }

    private static Booking sampleBooking() {
        return new Booking(
                "booking-1",
                "room-1",
                sampleRoom(),
                "unit-1",
                sampleRoomUnit(),
                "Guest",
                "guest@example.com",
                "+66800000000",
                "2026-01-01",
                "2026-01-02",
                "1500.00",
                BookingStatus.NEW,
                null,
                List.of(sampleBookingSegment()),
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private static PrintJob samplePrintJob() {
        return new PrintJob(
                "job-1",
                "printer-1",
                PrintDocumentType.KITCHEN_TICKET,
                "Kitchen ticket — Order #ORDER-1",
                PrintJobStatus.SENT,
                1,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private static MenuItemInput sampleMenuItemInput() {
        return new MenuItemInput("Pad Thai", "Stir-fried rice noodles", "Mains", BigDecimal.valueOf(250));
    }

    private static MenuItem sampleMenuItem() {
        return new MenuItem(
                "menu-1", "Pad Thai", "Stir-fried rice noodles", "Mains", MenuDepartment.KITCHEN, "250.00", true, OffsetDateTime.now());
    }

    private static Order sampleOrder() {
        return new Order(
                "order-1", null, null, "Walk-in", OrderStatus.OPEN, "user-1", "0.00", null, List.of(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private static PaymentsSummary samplePaymentsSummary() {
        ShiftTotals totals = new ShiftTotals("0.00", "0.00", "0.00", "0.00", 0);
        return new PaymentsSummary("2031-01-01", "2031-01-31", totals, "0.00");
    }
}
