package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.sunsetbeach.model.BookingGuestLinkInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestUpdateInput;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.DismissPrintJobsInput;
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
import com.sunsetbeach.model.PropertyMap;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitPositionInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentScheduleInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import com.sunsetbeach.model.SpaAppointmentTreatmentCreateInput;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.SpaTherapist;
import com.sunsetbeach.model.SwapSegmentRoomUnitInput;
import com.sunsetbeach.model.Table;
import com.sunsetbeach.model.TablePositionInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.AvailabilityService;
import com.sunsetbeach.service.BookingService;
import com.sunsetbeach.service.GuestService;
import com.sunsetbeach.service.MenuService;
import com.sunsetbeach.service.OrderService;
import com.sunsetbeach.service.PaymentService;
import com.sunsetbeach.service.PricingService;
import com.sunsetbeach.service.PrinterService;
import com.sunsetbeach.service.PropertyMapService;
import com.sunsetbeach.service.RoomService;
import com.sunsetbeach.service.RoomUnitService;
import com.sunsetbeach.service.ShiftService;
import com.sunsetbeach.service.SpaAppointmentService;
import com.sunsetbeach.service.SpaMapService;
import com.sunsetbeach.service.TableService;
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
import org.springframework.mock.web.MockMultipartFile;
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
            PropertyMapController.class,
            com.sunsetbeach.controller.AuditLogController.class,
            com.sunsetbeach.controller.MaintenanceTaskController.class,
            TableController.class,
            SpaController.class,
            GuestController.class
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
    private PropertyMapService propertyMapService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private com.sunsetbeach.service.BookingCalendarService bookingCalendarService;

    @MockitoBean
    private com.sunsetbeach.service.BookingOccupancyService bookingOccupancyService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private com.sunsetbeach.service.AuditLogService auditLogService;

    @MockitoBean
    private com.sunsetbeach.service.MaintenanceTaskService maintenanceTaskService;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private SpaAppointmentService spaAppointmentService;

    @MockitoBean
    private SpaMapService spaMapService;

    @MockitoBean
    private GuestService guestService;

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

    /** Overrides stubActiveUser()'s default (no functions) for the tests that need FUNCTION_<name> authorities granted. */
    private void stubActiveUserWithFunctions(String... functions) {
        UserEntity entity = new UserEntity();
        entity.setId("user-1");
        entity.setEmail("user-1@example.com");
        entity.setActive(true);
        entity.setJobFunctions(functions);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(entity));
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
        when(userService.setActive(eq("user-2"), anyString(), eq(false))).thenReturn(sampleUserUpdateResult());
        mockMvc.perform(patch("/users/user-2/active")
                        .header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserFunctions_withManagerToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/users/user-2/functions")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"functions\":[\"ENGINEER\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserFunctions_withAdminToken_isOk() throws Exception {
        when(userService.updateFunctions(eq("user-2"), any())).thenReturn(sampleUserUpdateResult());
        mockMvc.perform(patch("/users/user-2/functions")
                        .header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"functions\":[\"ENGINEER\"]}"))
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

    // --- GET /shifts requires MANAGER or above - CASHIER is not enough ---

    @Test
    void listShifts_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(get("/shifts").header("Authorization", token(Role.CASHIER))).andExpect(status().isForbidden());
    }

    @Test
    void listShifts_withManagerToken_isOk() throws Exception {
        when(shiftService.list(any(), any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/shifts").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
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
        when(printerService.listPrintJobs(any(), any(), anyBoolean(), eq(Role.WAITER))).thenReturn(List.of());
        mockMvc.perform(get("/print-jobs").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void listPrintJobs_withManagerToken_isOk() throws Exception {
        when(printerService.listPrintJobs(any(), any(), anyBoolean(), eq(Role.MANAGER))).thenReturn(List.of());
        mockMvc.perform(get("/print-jobs").header("Authorization", token(Role.MANAGER))).andExpect(status().isOk());
    }

    @Test
    void retryPrintJob_withWaiterToken_isOk() throws Exception {
        when(printerService.retryPrintJob(anyString(), eq(Role.WAITER))).thenReturn(samplePrintJob());
        mockMvc.perform(post("/print-jobs/job-1/retry").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    // Deliberately the same WAITER+ floor as retry, not a higher one - see dismissPrintJobs's
    // javadoc (PrinterService) and the SecurityConfig comment for why.
    @Test
    void dismissPrintJobs_withWaiterToken_isOk() throws Exception {
        when(printerService.dismissPrintJobs(any(), any(), eq(Role.WAITER), anyString())).thenReturn(List.of(samplePrintJob()));
        mockMvc.perform(post("/print-jobs/dismiss")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DismissPrintJobsInput(List.of("job-1")))))
                .andExpect(status().isOk());
    }

    // --- GET /room-units is open to any staff role, including WAITER - room numbers aren't
    // sensitive, and a CASHIER needs this list to pick a candidate before calling
    // PUT /bookings/{id}/room-unit below. Mutating endpoints stay MANAGER-only. ---

    @Test
    void listRoomUnits_withWaiterToken_isOk() throws Exception {
        when(roomUnitService.list(null)).thenReturn(List.of());
        mockMvc.perform(get("/room-units").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
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

    // --- PATCH /room-units/positions (property map editor's batch save) is MANAGER-only, same
    // tier as the rest of /room-units/** writes - falls through to that catch-all, no dedicated
    // rule in SecurityConfig, this is the regression test for that. ---

    @Test
    void saveRoomUnitPositions_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/room-units/positions")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(new RoomUnitPositionInput("unit-1").positionX(BigDecimal.valueOf(0.5)).positionY(BigDecimal.valueOf(0.5))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveRoomUnitPositions_withManagerToken_isOk() throws Exception {
        when(roomUnitService.savePositions(any())).thenReturn(List.of(sampleRoomUnit()));
        mockMvc.perform(patch("/room-units/positions")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(new RoomUnitPositionInput("unit-1").positionX(BigDecimal.valueOf(0.5)).positionY(BigDecimal.valueOf(0.5))))))
                .andExpect(status().isOk());
    }

    // --- PATCH /tables/positions (spa floor-plan editor's batch save) is MANAGER-only, same
    // tier and same "falls through to the PATCH /tables/** catch-all" reasoning as
    // PATCH /room-units/positions above. ---

    @Test
    void saveTablePositions_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/tables/positions")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                List.of(new TablePositionInput("table-1").positionX(BigDecimal.valueOf(0.5)).positionY(BigDecimal.valueOf(0.5))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveTablePositions_withManagerToken_isOk() throws Exception {
        when(tableService.savePositions(any())).thenReturn(List.of(sampleTable()));
        mockMvc.perform(patch("/tables/positions")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                List.of(new TablePositionInput("table-1").positionX(BigDecimal.valueOf(0.5)).positionY(BigDecimal.valueOf(0.5))))))
                .andExpect(status().isOk());
    }

    // --- Spa: booking/reading the half-hour grid is CASHIER+ - reception is the only surface in
    // v1, no therapist self-service (see JobFunction.THERAPIST). ---

    @Test
    void getSpaSchedule_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/spa-appointments?date=2027-01-01").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void getSpaSchedule_withCashierToken_isOk() throws Exception {
        when(spaAppointmentService.getSchedule(any())).thenReturn(sampleSpaSchedule());
        mockMvc.perform(get("/spa-appointments?date=2027-01-01").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void listSpaTherapists_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/spa-appointments/therapists").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void listSpaTherapists_withCashierToken_isOk() throws Exception {
        when(spaAppointmentService.listTherapists()).thenReturn(List.of(new SpaTherapist("user-1", "therapist@example.com")));
        mockMvc.perform(get("/spa-appointments/therapists").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void createSpaAppointment_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/spa-appointments")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SpaAppointmentCreateInput("booking-1", "table-1", "user-1", "menu-1", "2027-01-01", "10:00"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSpaAppointment_withCashierToken_isCreated() throws Exception {
        when(spaAppointmentService.create(any(), anyString())).thenReturn(new SpaAppointmentResult(sampleSpaAppointment(), null));
        mockMvc.perform(post("/spa-appointments")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SpaAppointmentCreateInput("booking-1", "table-1", "user-1", "menu-1", "2027-01-01", "10:00"))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateSpaAppointmentStatus_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/spa-appointments/appt-1/status")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSpaAppointmentStatus_withCashierToken_isOk() throws Exception {
        when(spaAppointmentService.updateStatus(eq("appt-1"), any(), anyString())).thenReturn(sampleSpaAppointment());
        mockMvc.perform(patch("/spa-appointments/appt-1/status")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED))))
                .andExpect(status().isOk());
    }

    @Test
    void updateSpaAppointmentSchedule_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/spa-appointments/appt-1/schedule")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentScheduleInput("table-1", "user-1", "2027-01-01", "10:00"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSpaAppointmentSchedule_withCashierToken_isOk() throws Exception {
        when(spaAppointmentService.updateSchedule(eq("appt-1"), any(), anyString())).thenReturn(sampleSpaAppointment());
        mockMvc.perform(patch("/spa-appointments/appt-1/schedule")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentScheduleInput("table-1", "user-1", "2027-01-01", "10:00"))))
                .andExpect(status().isOk());
    }

    @Test
    void addSpaAppointmentTreatment_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/spa-appointments/appt-1/treatments")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentTreatmentCreateInput("menu-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSpaAppointmentTreatment_withCashierToken_isCreated() throws Exception {
        when(spaAppointmentService.addTreatment(eq("appt-1"), any(), anyString())).thenReturn(sampleSpaAppointment());
        mockMvc.perform(post("/spa-appointments/appt-1/treatments")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SpaAppointmentTreatmentCreateInput("menu-1"))))
                .andExpect(status().isCreated());
    }

    @Test
    void removeSpaAppointmentTreatment_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(delete("/spa-appointments/appt-1/treatments/treatment-1").header("Authorization", token(Role.WAITER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeSpaAppointmentTreatment_withCashierToken_isOk() throws Exception {
        when(spaAppointmentService.removeTreatment(eq("appt-1"), eq("treatment-1"), anyString())).thenReturn(sampleSpaAppointment());
        mockMvc.perform(delete("/spa-appointments/appt-1/treatments/treatment-1").header("Authorization", token(Role.CASHIER)))
                .andExpect(status().isOk());
    }

    // --- Property map: viewing is CASHIER+ (same floor as GET /bookings/today), replacing the
    // background image is MANAGER+ (same floor as POST /rooms/{id}/images). ---

    @Test
    void getPropertyMap_withCashierToken_isOk() throws Exception {
        when(propertyMapService.get()).thenReturn(samplePropertyMap());
        mockMvc.perform(get("/property-map").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void getPropertyMap_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/property-map").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void getPropertyMapImage_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/property-map/image").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void uploadPropertyMapImage_withCashierToken_isForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.jpg", "image/jpeg", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/property-map/image").file(file).header("Authorization", token(Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadPropertyMapImage_withManagerToken_isCreated() throws Exception {
        when(propertyMapService.uploadImage(any())).thenReturn(samplePropertyMap());
        MockMultipartFile file = new MockMultipartFile("file", "plan.jpg", "image/jpeg", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/property-map/image").file(file).header("Authorization", token(Role.MANAGER)))
                .andExpect(status().isCreated());
    }

    // --- Spa map: same CASHIER-viewing/MANAGER-replacing split as the property map just above -
    // mirrored deliberately, see SpaMapService's own class javadoc. ---

    @Test
    void getSpaMap_withCashierToken_isOk() throws Exception {
        when(spaMapService.get()).thenReturn(sampleSpaMap());
        mockMvc.perform(get("/spa-map").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void getSpaMap_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/spa-map").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void getSpaMapImage_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/spa-map/image").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void uploadSpaMapImage_withCashierToken_isForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.jpg", "image/jpeg", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/spa-map/image").file(file).header("Authorization", token(Role.CASHIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadSpaMapImage_withManagerToken_isCreated() throws Exception {
        when(spaMapService.uploadImage(any())).thenReturn(sampleSpaMap());
        MockMultipartFile file = new MockMultipartFile("file", "plan.jpg", "image/jpeg", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/spa-map/image").file(file).header("Authorization", token(Role.MANAGER)))
                .andExpect(status().isCreated());
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

    // --- PUT /bookings/{id}/segments/{segmentId}/room-unit and POST .../swap-room-unit are the
    // same CASHIER+ tier as PUT /bookings/{id}/room-unit just above, with their own explicit
    // rules in SecurityConfig (three-segment paths past "/bookings/" need one). ---

    @Test
    void assignBookingSegmentRoomUnit_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(put("/bookings/booking-1/segments/segment-1/room-unit")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitAssignmentInput().roomUnitId("unit-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignBookingSegmentRoomUnit_withCashierToken_isOk() throws Exception {
        when(bookingService.assignSegmentRoomUnit(eq("booking-1"), eq("segment-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(put("/bookings/booking-1/segments/segment-1/room-unit")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoomUnitAssignmentInput().roomUnitId("unit-1"))))
                .andExpect(status().isOk());
    }

    @Test
    void swapBookingSegmentRoomUnit_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/bookings/booking-1/segments/segment-1/swap-room-unit")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SwapSegmentRoomUnitInput("segment-2"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void swapBookingSegmentRoomUnit_withCashierToken_isOk() throws Exception {
        when(bookingService.swapSegmentRoomUnit(eq("booking-1"), eq("segment-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(post("/bookings/booking-1/segments/segment-1/swap-room-unit")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SwapSegmentRoomUnitInput("segment-2"))))
                .andExpect(status().isOk());
    }

    // --- PUT /bookings/{id}/guest requires CASHIER or above, same tier as PUT /bookings/{id}/
    // room-unit just above - and, unlike that endpoint, relinking to a different guest needs no
    // confirmation/state check, so there's no equivalent of the room-unit 409 conflict cases to
    // cover here. ---

    @Test
    void assignBookingGuest_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(put("/bookings/booking-1/guest")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingGuestLinkInput().guestId("guest-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignBookingGuest_withCashierToken_isOk() throws Exception {
        when(bookingService.assignGuest(eq("booking-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(put("/bookings/booking-1/guest")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingGuestLinkInput().guestId("guest-1"))))
                .andExpect(status().isOk());
    }

    @Test
    void assignBookingGuest_withExplicitNull_unlinks() throws Exception {
        when(bookingService.assignGuest(eq("booking-1"), any())).thenReturn(sampleBooking());
        mockMvc.perform(put("/bookings/booking-1/guest")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestId\":null}"))
                .andExpect(status().isOk());
    }

    // --- /guests/** is CASHIER or above across every operation - search, create, and the
    // per-guest card (read/update/delete) all share the same floor as the rest of front-desk
    // reservation work (GET /bookings, POST /bookings/staff). ---

    @Test
    void searchGuests_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/guests").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void searchGuests_withCashierToken_isOk() throws Exception {
        when(guestService.search(any())).thenReturn(List.of(sampleGuest()));
        mockMvc.perform(get("/guests").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void createGuest_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(post("/guests")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuestCreateInput("Jane Doe"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGuest_withCashierToken_isCreated() throws Exception {
        when(guestService.create(any())).thenReturn(sampleGuest());
        mockMvc.perform(post("/guests")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuestCreateInput("Jane Doe"))))
                .andExpect(status().isCreated());
    }

    @Test
    void getGuest_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(get("/guests/guest-1").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void getGuest_withCashierToken_isOk() throws Exception {
        when(guestService.getDetail("guest-1")).thenReturn(sampleGuestDetail());
        mockMvc.perform(get("/guests/guest-1").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
    }

    @Test
    void updateGuest_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(patch("/guests/guest-1")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuestUpdateInput("Jane Doe"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateGuest_withCashierToken_isOk() throws Exception {
        when(guestService.update(eq("guest-1"), any())).thenReturn(sampleGuest());
        mockMvc.perform(patch("/guests/guest-1")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuestUpdateInput("Jane Doe"))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteGuest_withWaiterToken_isForbidden() throws Exception {
        mockMvc.perform(delete("/guests/guest-1").header("Authorization", token(Role.WAITER))).andExpect(status().isForbidden());
    }

    @Test
    void deleteGuest_withCashierToken_isOk() throws Exception {
        mockMvc.perform(delete("/guests/guest-1").header("Authorization", token(Role.CASHIER))).andExpect(status().isOk());
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

    // --- Maintenance tasks: filing/reading are open to any authenticated staff role, blocking the
    // room is MANAGER+, status transitions are FUNCTION_ENGINEER *or* MANAGER+ (a manager is the
    // fallback if nobody currently holds the function - see SecurityConfig#engineerOrManagerPlus) ---

    @Test
    void listMaintenanceTasks_withWaiterToken_isOk() throws Exception {
        mockMvc.perform(get("/maintenance-tasks").header("Authorization", token(Role.WAITER))).andExpect(status().isOk());
    }

    @Test
    void createMaintenanceTask_withWaiterToken_isCreated() throws Exception {
        when(maintenanceTaskService.create(eq("unit-1"), eq("AC is leaking"), any(), any())).thenReturn(sampleMaintenanceTask());
        mockMvc.perform(multipart("/maintenance-tasks")
                        .param("roomUnitId", "unit-1")
                        .param("description", "AC is leaking")
                        .header("Authorization", token(Role.WAITER)))
                .andExpect(status().isCreated());
    }

    @Test
    void blockMaintenanceTaskRoom_withManagerToken_isOk() throws Exception {
        when(maintenanceTaskService.addBlock(eq("task-1"), any()))
                .thenReturn(new com.sunsetbeach.model.MaintenanceTaskBlockResult(sampleMaintenanceTask(), sampleRoomUnitBlockResult()));
        mockMvc.perform(post("/maintenance-tasks/task-1/block")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"2031-01-01\",\"toDate\":\"2031-01-02\",\"reason\":\"repair\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void blockMaintenanceTaskRoom_withCashierToken_isForbidden() throws Exception {
        mockMvc.perform(post("/maintenance-tasks/task-1/block")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"2031-01-01\",\"toDate\":\"2031-01-02\",\"reason\":\"repair\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMaintenanceTaskStatus_withManagerToken_isOk() throws Exception {
        when(maintenanceTaskService.updateStatus(eq("task-1"), any())).thenReturn(sampleMaintenanceTask());
        mockMvc.perform(patch("/maintenance-tasks/task-1/status")
                        .header("Authorization", token(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMaintenanceTaskStatus_withCashierTokenAndNoFunction_isForbidden() throws Exception {
        mockMvc.perform(patch("/maintenance-tasks/task-1/status")
                        .header("Authorization", token(Role.CASHIER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMaintenanceTaskStatus_withWaiterTokenAndEngineerFunction_isOk() throws Exception {
        // The point of this test: a WAITER (the lowest role) with the ENGINEER function granted
        // can still do this - the function is independent of RoleHierarchy, not a role in disguise.
        stubActiveUserWithFunctions("ENGINEER");
        when(maintenanceTaskService.updateStatus(eq("task-1"), any())).thenReturn(sampleMaintenanceTask());
        mockMvc.perform(patch("/maintenance-tasks/task-1/status")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMaintenanceTaskStatus_withWaiterTokenAndHousekeeperFunction_isForbidden() throws Exception {
        // Confirms the gate checks specifically for ENGINEER, not "has any function at all".
        stubActiveUserWithFunctions("HOUSEKEEPER");
        mockMvc.perform(patch("/maintenance-tasks/task-1/status")
                        .header("Authorization", token(Role.WAITER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    private static com.sunsetbeach.model.MaintenanceTask sampleMaintenanceTask() {
        return new com.sunsetbeach.model.MaintenanceTask(
                "task-1", "unit-1", "room-1", "Ocean View Suite", "203", "AC is leaking",
                com.sunsetbeach.model.MaintenanceTaskStatus.OPEN, null, "user-1", "user-1@example.com", List.of(), OffsetDateTime.now(), null);
    }

    private static com.sunsetbeach.model.RoomUnitBlockResult sampleRoomUnitBlockResult() {
        return new com.sunsetbeach.model.RoomUnitBlockResult(
                new com.sunsetbeach.model.RoomUnitBlock("block-1", "unit-1", "2031-01-01", "2031-01-02", "repair", OffsetDateTime.now()),
                null, List.of(), List.of());
    }

    private static RoomUnit sampleRoomUnit() {
        return new RoomUnit("unit-1", "room-1", "203", true, HousekeepingStatus.CLEAN, OffsetDateTime.now());
    }

    private static PropertyMap samplePropertyMap() {
        return new PropertyMap(null, null, List.of());
    }

    private static com.sunsetbeach.model.SpaMap sampleSpaMap() {
        return new com.sunsetbeach.model.SpaMap(null, null);
    }

    private static Room sampleRoom() {
        return new Room("room-1", "Ocean View Suite", "A lovely room", 2, 3, "1500.00", List.of(), OffsetDateTime.now());
    }

    private static com.sunsetbeach.model.User sampleUser() {
        return new com.sunsetbeach.model.User("user-2", "user-2@example.com", Role.CASHIER, true, List.of(), OffsetDateTime.now());
    }

    private static com.sunsetbeach.model.UserUpdateResult sampleUserUpdateResult() {
        return new com.sunsetbeach.model.UserUpdateResult(sampleUser(), null);
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
                null,
                null,
                "2026-01-01",
                "2026-01-02",
                "1500.00",
                BookingStatus.NEW,
                null,
                OccupancyStatus.EXPECTED,
                null,
                null,
                List.of(sampleBookingSegment()),
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private static Guest sampleGuest() {
        return new Guest("guest-1", "Jane Doe", "jane@example.com", "+66800000000", null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private static GuestDetail sampleGuestDetail() {
        return new GuestDetail(
                "guest-1", "Jane Doe", "jane@example.com", "+66800000000", null, OffsetDateTime.now(), OffsetDateTime.now(), List.of());
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

    private static Table sampleTable() {
        return new Table("table-1", Zone.SPA, "Spa Table 1", 1, true);
    }

    private static SpaAppointment sampleSpaAppointment() {
        return new SpaAppointment(
                "appt-1",
                "booking-1",
                "Guest",
                "table-1",
                "Spa Table 1",
                "user-1",
                "therapist@example.com",
                List.of(new SpaAppointmentTreatment("treatment-1", "menu-1", "Massage", 60, "1500.00")),
                "2027-01-01",
                "10:00",
                60,
                SpaAppointmentStatus.BOOKED,
                null,
                List.of(),
                "user-2",
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private static SpaSchedule sampleSpaSchedule() {
        return new SpaSchedule("2027-01-01", "09:00", "20:00", 30, List.of(sampleTable()), List.of(sampleSpaAppointment()));
    }

    private static Order sampleOrder() {
        return new Order(
                "order-1",
                null,
                null,
                "Walk-in",
                OrderStatus.OPEN,
                "user-1",
                "user-1@example.com",
                "0.00",
                null,
                List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null);
    }

    private static PaymentsSummary samplePaymentsSummary() {
        ShiftTotals totals = new ShiftTotals("0.00", "0.00", "0.00", "0.00", 0);
        return new PaymentsSummary("2031-01-01", "2031-01-31", totals, "0.00");
    }
}
