package com.sunsetbeach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.config.JacksonConfig;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.MenuItem;
import com.sunsetbeach.model.MenuItemInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PaymentsSummary;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftTotals;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.RestAccessDeniedHandler;
import com.sunsetbeach.security.RestAuthEntryPoint;
import com.sunsetbeach.security.SecurityConfig;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.MenuService;
import com.sunsetbeach.service.OrderService;
import com.sunsetbeach.service.PaymentService;
import com.sunsetbeach.service.ShiftService;
import com.sunsetbeach.service.UserService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
@WebMvcTest(controllers = {MenuController.class, OrderController.class, UserController.class, ShiftController.class, PaymentController.class})
@Import({SecurityConfig.class, JwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class, JacksonConfig.class})
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> JWT_SECRET);
        registry.add("app.security.jwt-ttl-days", () -> "7");
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

    private static MenuItemInput sampleMenuItemInput() {
        return new MenuItemInput("Pad Thai", "Stir-fried rice noodles", "Mains", BigDecimal.valueOf(250));
    }

    private static MenuItem sampleMenuItem() {
        return new MenuItem("menu-1", "Pad Thai", "Stir-fried rice noodles", "Mains", "250.00", true, OffsetDateTime.now());
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
