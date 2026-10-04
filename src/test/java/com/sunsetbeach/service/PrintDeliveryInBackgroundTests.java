package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.PrintJobRepository;
import com.sunsetbeach.repository.PrinterRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * A slow or unreachable printer never holds up the POS action that queued the print. Sending an
 * order used to attempt delivery inside the request, so with a dead printer every send, void and
 * close waited out the 2-second connect timeout. Here the printer takes 3 seconds per send: the
 * send must come back well before that, and the ticket must still reach the printer afterwards.
 *
 * <p>No {@code @Transactional} on the class: delivery starts after the operation's transaction
 * commits, which a rolled-back test transaction never does. Rows are removed in {@code @AfterEach}.
 */
@SpringBootTest
class PrintDeliveryInBackgroundTests extends AbstractIntegrationTest {

    private static final Duration SLOW_PRINTER = Duration.ofSeconds(3);

    @MockitoBean
    private PrinterClient printerClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PrinterRepository printerRepository;

    @Autowired
    private PrintJobRepository printJobRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity waiter;
    private MenuItemEntity kitchenItem;
    private PrinterEntity createdPrinter;
    private String orderId;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            Thread.sleep(SLOW_PRINTER.toMillis());
            return null;
        }).when(printerClient).send(anyString(), anyInt(), any());

        UserEntity user = new UserEntity();
        user.setEmail("waiter-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.WAITER);
        waiter = userRepository.saveAndFlush(user);

        MenuItemEntity item = new MenuItemEntity();
        item.setName("Pad Thai " + UUID.randomUUID());
        item.setDescription("test item");
        item.setCategory("Test");
        item.setDepartment(MenuDepartment.KITCHEN);
        item.setPrice(new BigDecimal("150.00"));
        item.setAvailable(true);
        kitchenItem = menuItemRepository.saveAndFlush(item);

        // One active printer per department is allowed; the client is mocked, so an existing one
        // serves just as well and is left alone.
        if (printerRepository.findByDepartmentAndIsActiveTrue(PrinterDepartment.KITCHEN).isEmpty()) {
            PrinterEntity printer = new PrinterEntity();
            printer.setName("Background delivery test printer");
            printer.setDepartment(PrinterDepartment.KITCHEN);
            printer.setHost("127.0.0.1");
            printer.setPort(9100);
            printer.setActive(true);
            createdPrinter = printerRepository.saveAndFlush(printer);
        }
    }

    @AfterEach
    void cleanUp() {
        if (orderId != null) {
            jdbcTemplate.update("DELETE FROM \"PrintJob\" WHERE summary LIKE ?", "%Order #" + orderNumber() + "%");
            jdbcTemplate.update("DELETE FROM \"OrderItem\" WHERE \"orderId\" = ?", orderId);
            jdbcTemplate.update("DELETE FROM \"Order\" WHERE id = ?", orderId);
        }
        menuItemRepository.deleteById(kitchenItem.getId());
        userRepository.deleteById(waiter.getId());
        if (createdPrinter != null) {
            printerRepository.deleteById(createdPrinter.getId());
        }
    }

    @Test
    void sendingAnOrderDoesNotWaitForTheKitchenPrinter_butTheTicketStillPrints() throws Exception {
        Order order = orderService.create(new OrderCreateInput(), waiter.getId());
        orderId = order.getId();
        orderService.addItems(orderId, List.of(new OrderItemInput(kitchenItem.getId(), 1)));

        long started = System.nanoTime();
        Order sent = orderService.update(orderId, new OrderUpdateInput().status(OrderStatus.SENT));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(sent.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(elapsed).isLessThan(SLOW_PRINTER.dividedBy(3));

        PrintJobEntity job = awaitTicket(job0 -> job0.getStatus() == PrintJobStatus.SENT, Duration.ofSeconds(15));
        assertThat(job.getAttempts()).isEqualTo(1);
    }

    private long orderNumber() {
        return orderService.getById(orderId).getNumber();
    }

    private PrintJobEntity awaitTicket(Predicate<PrintJobEntity> done, Duration timeout) throws InterruptedException {
        String label = "Order #" + orderNumber();
        long deadline = System.nanoTime() + timeout.toNanos();
        PrintJobEntity latest = null;
        while (System.nanoTime() < deadline) {
            latest = printJobRepository.findAll().stream()
                    .filter(j -> j.getSummary().contains(label))
                    .findFirst()
                    .orElse(null);
            if (latest != null && done.test(latest)) {
                return latest;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Ticket for " + label + " never reached the expected state; last seen: "
                + (latest == null ? "no job" : latest.getStatus() + " after " + latest.getAttempts() + " attempt(s)"));
    }
}
