package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - {@code AuditLogService.record}
 * commits in its own {@code REQUIRES_NEW} transaction (see its javadoc) and would survive this
 * test's own rollback anyway, so cleanup is manual, by id, in {@link #cleanUp()}.
 */
@SpringBootTest
class OrderShiftAuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdOrderIds = new ArrayList<>();
    private final List<String> createdShiftIds = new ArrayList<>();
    private final List<String> createdBookingIds = new ArrayList<>();
    private final List<String> createdRoomIds = new ArrayList<>();
    private String cashierId;
    private String menuItemId;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("audit-order-shift-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        cashierId = userRepository.saveAndFlush(user).getId();

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setName("Audit Test Item");
        menuItem.setDescription("Used only by OrderShiftAuditLogTests");
        menuItem.setCategory("Test");
        menuItem.setPrice(new BigDecimal("150.00"));
        menuItem.setAvailable(true);
        menuItemId = menuItemRepository.saveAndFlush(menuItem).getId();

        StaffPrincipal principal = new StaffPrincipal(cashierId, user.getEmail(), Role.CASHIER);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CASHIER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String orderId : createdOrderIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.ORDER, orderId));
            paymentRepository.deleteAll(paymentRepository.findAll().stream().filter(p -> orderId.equals(p.getOrderId())).toList());
            orderItemRepository.deleteAll(orderItemRepository.findByOrderId(orderId));
            orderRepository.deleteById(orderId);
        }
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.BOOKING, bookingId));
            bookingRepository.deleteById(bookingId);
        }
        for (String roomId : createdRoomIds) {
            for (var unit : roomUnitRepository.findByRoomId(roomId)) {
                roomUnitRepository.deleteById(unit.getId());
            }
            roomRepository.deleteById(roomId);
        }
        for (String shiftId : createdShiftIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.SHIFT, shiftId));
            shiftRepository.deleteById(shiftId);
        }
        if (menuItemId != null) {
            menuItemRepository.deleteById(menuItemId);
        }
        if (cashierId != null) {
            userRepository.deleteById(cashierId);
        }
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }

    private Order openOrderWithOneItem() {
        Order order = orderService.create(new OrderCreateInput(), cashierId);
        createdOrderIds.add(order.getId());
        return orderService.addItems(order.getId(), List.of(new OrderItemInput(menuItemId, 1)));
    }

    @Test
    void closeOrder_cash_writesOrderClosedEntryOnly() {
        Shift shift = shiftService.open(cashierId, new ShiftOpenInput());
        createdShiftIds.add(shift.getId());
        Order order = openOrderWithOneItem();

        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashierId);

        // Exactly one entry, and it's tagged under the ORDER entity, not BOOKING - a cash close
        // has no booking to post a room charge against, so ROOM_CHARGE_POSTED (which is always
        // filed under AuditEntityType.BOOKING) can't appear in this list by construction.
        List<AuditLogEntity> orderEntries = entriesFor(AuditEntityType.ORDER, order.getId());
        assertThat(orderEntries).hasSize(1);
        assertThat(orderEntries.get(0).getAction()).isEqualTo(AuditAction.ORDER_CLOSED);
        assertThat(orderEntries.get(0).getSummary()).contains("CASH").contains("150.00");
    }

    @Test
    void closeOrder_roomCharge_writesOrderClosedAndRoomChargePostedEntries() {
        RoomEntity room = new RoomEntity();
        room.setName("Audit Order Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by OrderShiftAuditLogTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());
        var unit = new com.sunsetbeach.entity.RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Audit Order Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);

        Booking booking = bookingService.createStaffBooking(new StaffBookingCreateInput(
                savedRoom.getId(), "Guest", LocalDate.now().plusDays(3).toString(), LocalDate.now().plusDays(4).toString()));
        createdBookingIds.add(booking.getId());

        Shift shift = shiftService.open(cashierId, new ShiftOpenInput());
        createdShiftIds.add(shift.getId());
        Order order = openOrderWithOneItem();

        CloseOrderInput input = new CloseOrderInput(PaymentMethod.ROOM_CHARGE);
        input.setBookingId(booking.getId());
        orderService.close(order.getId(), input, cashierId);

        List<AuditLogEntity> orderEntries = entriesFor(AuditEntityType.ORDER, order.getId());
        assertThat(orderEntries).hasSize(1);
        assertThat(orderEntries.get(0).getSummary()).contains("ROOM_CHARGE").contains("Guest");

        List<AuditLogEntity> bookingEntries = entriesFor(AuditEntityType.BOOKING, booking.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_CHARGE_POSTED)
                .toList();
        assertThat(bookingEntries).hasSize(1);
        assertThat(bookingEntries.get(0).getSummary()).contains("150.00").contains("Guest");
    }

    @Test
    void cancelOrder_writesOrderCancelledEntry() {
        Order order = openOrderWithOneItem();

        orderService.cancel(order.getId());

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.ORDER, order.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ORDER_CANCELLED)
                .toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    void openAndCloseShift_writeShiftOpenedAndClosedEntries() {
        Shift shift = shiftService.open(cashierId, new ShiftOpenInput().openingCashFloat(new BigDecimal("500.00")));
        createdShiftIds.add(shift.getId());

        List<AuditLogEntity> openEntries = entriesFor(AuditEntityType.SHIFT, shift.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.SHIFT_OPENED)
                .toList();
        assertThat(openEntries).hasSize(1);
        assertThat(openEntries.get(0).getSummary()).contains("500.00");

        ShiftCloseInput closeInput = new ShiftCloseInput();
        closeInput.setClosingCashCounted(new BigDecimal("500.00"));
        shiftService.close(shift.getId(), closeInput);

        List<AuditLogEntity> closeEntries = entriesFor(AuditEntityType.SHIFT, shift.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.SHIFT_CLOSED)
                .toList();
        assertThat(closeEntries).hasSize(1);
        assertThat(closeEntries.get(0).getSummary()).contains("Shift closed");
    }

    @Test
    void closeShift_withDiscrepancy_mentionsItInSummary() {
        Shift shift = shiftService.open(cashierId, new ShiftOpenInput().openingCashFloat(new BigDecimal("500.00")));
        createdShiftIds.add(shift.getId());

        ShiftCloseInput closeInput = new ShiftCloseInput();
        // Nothing was actually received, so expected cash is exactly the 500.00 float - counting
        // 450.00 is a deliberate 50.00 shortfall.
        closeInput.setClosingCashCounted(new BigDecimal("450.00"));
        shiftService.close(shift.getId(), closeInput);

        List<AuditLogEntity> closeEntries = entriesFor(AuditEntityType.SHIFT, shift.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.SHIFT_CLOSED)
                .toList();
        assertThat(closeEntries).hasSize(1);
        assertThat(closeEntries.get(0).getSummary()).contains("discrepancy").contains("-50.00");
    }

    @Test
    void exportShift_writesShiftExportedEntry() {
        Shift shift = shiftService.open(cashierId, new ShiftOpenInput());
        createdShiftIds.add(shift.getId());

        shiftService.exportCsv(shift.getId());

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.SHIFT, shift.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.SHIFT_EXPORTED)
                .toList();
        assertThat(entries).hasSize(1);
    }
}
