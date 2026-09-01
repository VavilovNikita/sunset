package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DB-backed against the real dev Postgres, deliberately NOT {@code @Transactional} - same
 * reasoning as {@code BookingAvailabilityEngineTests}: the race under test needs two genuinely
 * separate top-level transactions/connections, which a wrapping test transaction would prevent
 * the worker threads' setup visibility from working correctly. Cleans up what it wrote in
 * {@link #cleanUp()} instead of relying on rollback.
 *
 * <p>Covers the fix for a real gap: {@code OrderService.close()}'s read-then-write status check
 * is not atomic on its own, so two concurrent closes of the same order could both observe OPEN
 * before either commits and both post a Payment - double-charging the guest's folio for a
 * ROOM_CHARGE close. The unique constraint on {@code Payment.orderId} (migration V14) is the
 * actual guard; this proves it holds under real concurrency, not just that the code compiles.
 */
@SpringBootTest
class OrderDoubleCloseRaceTests extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ShiftService shiftService;

    private String cashierId;
    private String shiftId;
    private String orderId;
    private String menuItemId;

    @AfterEach
    void cleanUp() {
        if (shiftId != null) {
            paymentRepository.deleteAll(paymentRepository.findByShiftId(shiftId));
        }
        if (orderId != null) {
            orderItemRepository.deleteAll(orderItemRepository.findByOrderId(orderId));
            orderRepository.deleteById(orderId);
        }
        if (shiftId != null) {
            shiftRepository.deleteById(shiftId);
        }
        if (menuItemId != null) {
            menuItemRepository.deleteById(menuItemId);
        }
        if (cashierId != null) {
            userRepository.deleteById(cashierId);
        }
    }

    @Test
    void concurrentClose_exactlyOneSucceeds_andExactlyOnePaymentIsPersisted() throws Exception {
        UserEntity cashier = new UserEntity();
        cashier.setEmail("double-close-" + UUID.randomUUID() + "@example.com");
        cashier.setPasswordHash("irrelevant-for-this-test");
        cashier.setRole(Role.CASHIER);
        cashierId = userRepository.saveAndFlush(cashier).getId();

        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setName("Race Test Item");
        menuItem.setDescription("Used only by OrderDoubleCloseRaceTests");
        menuItem.setCategory("Test");
        menuItem.setPrice(new BigDecimal("100.00"));
        menuItem.setAvailable(true);
        menuItemId = menuItemRepository.saveAndFlush(menuItem).getId();

        shiftId = shiftService.open(cashierId, new com.sunsetbeach.model.ShiftOpenInput()).getId();

        Order order = orderService.create(new OrderCreateInput(), cashierId);
        orderId = order.getId();
        orderService.addItems(orderId, List.of(new OrderItemInput(menuItemId, 1)));

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Order> closeTask = () -> {
            barrier.await();
            return orderService.close(orderId, new CloseOrderInput(PaymentMethod.CASH), cashierId);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Order> first = executor.submit(closeTask);
            Future<Order> second = executor.submit(closeTask);

            int succeeded = 0;
            int conflicted = 0;
            for (Future<Order> future : List.of(first, second)) {
                try {
                    future.get();
                    succeeded++;
                } catch (Exception e) {
                    if (e.getCause() instanceof ConflictException) {
                        conflicted++;
                    } else {
                        throw e;
                    }
                }
            }

            assertThat(succeeded).isEqualTo(1);
            assertThat(conflicted).isEqualTo(1);
        } finally {
            executor.shutdown();
        }

        assertThat(paymentRepository.findAll().stream().filter(p -> p.getOrderId().equals(orderId)).count()).isEqualTo(1);
    }
}
