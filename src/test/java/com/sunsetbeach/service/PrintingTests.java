package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderUpdateInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJob;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.Printer;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.PrinterInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.PrintJobRepository;
import com.sunsetbeach.repository.PrinterRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): the network-printing feature end
 * to end - kitchen/bar ticket routing by {@code MenuItem.department}, the fail-open contract
 * with a genuinely unreachable printer (operation succeeds, {@code PrintJob} lands in
 * {@code FAILED}), manual retry recovering once the printer is reachable again, and the Z-report
 * keeping ROOM_CHARGE out of the received-money total (same rule as {@link ShiftExportTests}'
 * CSV coverage, now for the printed version). {@code app.printing.max-attempts=1} is forced for
 * the whole class so a single failed automatic attempt is immediately terminal ({@code FAILED})
 * without needing to wait out multiple retries; {@code retryPendingJobs}' background sweep is
 * pushed out to effectively never fire, so it can't race these assertions.
 */
@SpringBootTest
@Transactional
class PrintingTests extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.printing.max-attempts", () -> "1");
        registry.add("app.printing.connect-timeout-ms", () -> "1000");
        registry.add("app.printing.retry-interval-ms", () -> "3600000");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PrinterRepository printerRepository;

    @Autowired
    private PrintJobRepository printJobRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    private UserEntity cashier;
    private MenuItemEntity kitchenItem;
    private MenuItemEntity barItem;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("cashier-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        cashier = userRepository.saveAndFlush(user);

        kitchenItem = persistMenuItem("Caesar Salad", MenuDepartment.KITCHEN, "100.00");
        barItem = persistMenuItem("Mojito", MenuDepartment.BAR, "100.00");
    }

    // --- Ticket routing by MenuItem.department ---

    @Test
    void sendOrder_mixedItems_splitsTicketsByDepartment_kitchenTicketHasNoPrices() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            PrinterEntity kitchenPrinter = persistPrinter(PrinterDepartment.KITCHEN, fake.port());
            PrinterEntity barPrinter = persistPrinter(PrinterDepartment.BAR, fake.port());

            Order order = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(
                    order.getId(),
                    List.of(new OrderItemInput(kitchenItem.getId(), 2), new OrderItemInput(barItem.getId(), 1)));
            sendOrder(order.getId());

            List<PrintJobEntity> jobs = jobsFor(order.getId());
            assertThat(jobs).hasSize(2);

            PrintJobEntity kitchenJob = jobs.stream()
                    .filter(j -> j.getSummary().startsWith("Kitchen ticket"))
                    .findFirst()
                    .orElseThrow();
            PrintJobEntity barJob = jobs.stream()
                    .filter(j -> j.getSummary().startsWith("Bar ticket"))
                    .findFirst()
                    .orElseThrow();

            assertThat(kitchenJob.getDocumentType()).isEqualTo(PrintDocumentType.KITCHEN_TICKET);
            assertThat(kitchenJob.getPrinterId()).isEqualTo(kitchenPrinter.getId());
            assertThat(barJob.getDocumentType()).isEqualTo(PrintDocumentType.BAR_TICKET);
            assertThat(barJob.getPrinterId()).isEqualTo(barPrinter.getId());

            String kitchenText = decode(kitchenJob);
            assertThat(kitchenText).contains("2x Caesar Salad");
            assertThat(kitchenText).doesNotContain("Mojito");
            assertThat(kitchenText).doesNotContain("100.00"); // no prices on a kitchen ticket

            String barText = decode(barJob);
            assertThat(barText).contains("1x Mojito");
            assertThat(barText).doesNotContain("Caesar Salad");
            assertThat(barText).doesNotContain("100.00"); // no prices on a bar ticket either

            // Both printers were reachable - delivery actually succeeded, not just queued.
            assertThat(kitchenJob.getStatus()).isEqualTo(PrintJobStatus.SENT);
            assertThat(barJob.getStatus()).isEqualTo(PrintJobStatus.SENT);
        }
    }

    @Test
    void sendOrder_drinksOnly_printsNothingToTheKitchen() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            persistPrinter(PrinterDepartment.BAR, fake.port());
            // Deliberately no KITCHEN printer configured for this test.

            Order order = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(order.getId(), List.of(new OrderItemInput(barItem.getId(), 1)));
            sendOrder(order.getId());

            List<PrintJobEntity> jobs = jobsFor(order.getId());
            assertThat(jobs).hasSize(1);
            assertThat(jobs.get(0).getDocumentType()).isEqualTo(PrintDocumentType.BAR_TICKET);
            assertThat(jobs.get(0).getSummary()).startsWith("Bar ticket");
        }
    }

    // --- Re-order after send: the second ticket carries only the delta, never the whole order again ---

    @Test
    void reorderAfterSend_secondTicketContainsOnlyTheNewlyAddedItem_notTheWholeOrderAgain() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            persistPrinter(PrinterDepartment.KITCHEN, fake.port());

            Order order = orderService.create(new OrderCreateInput(), cashier.getId());
            // Tapping the same item three times, one at a time - mirrors the exact staff workflow
            // this covers, and exercises the merge from OrderItemMergingTests along the way.
            orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            sendOrder(order.getId());

            List<PrintJobEntity> afterFirstSend = jobsFor(order.getId());
            assertThat(afterFirstSend).hasSize(1);
            assertThat(decode(afterFirstSend.get(0))).contains("3x Caesar Salad");

            // A round after the ticket already went out - no "Send to kitchen" button exists for
            // this in the UI at all; the backend must dispatch it on its own.
            Order reordered = orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            assertThat(reordered.getItems()).hasSize(2); // the sent 3x line, plus a new unsent 1x line

            List<PrintJobEntity> afterReorder = jobsFor(order.getId());
            assertThat(afterReorder).hasSize(2); // one ticket per send, not one growing job
            PrintJobEntity secondTicket = afterReorder.stream()
                    .filter(j -> !j.getId().equals(afterFirstSend.get(0).getId()))
                    .findFirst()
                    .orElseThrow();
            String secondText = decode(secondTicket);
            assertThat(secondText).contains("1x Caesar Salad");
            // The critical assertion: the kitchen must not be told to re-cook the original three.
            assertThat(secondText).doesNotContain("3x Caesar Salad");
            assertThat(secondText).doesNotContain("4x Caesar Salad");
        }
    }

    @Test
    void reorderAfterSend_departmentSplitStillAppliesToTheDelta() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            PrinterEntity kitchenPrinter = persistPrinter(PrinterDepartment.KITCHEN, fake.port());
            PrinterEntity barPrinter = persistPrinter(PrinterDepartment.BAR, fake.port());

            Order order = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            sendOrder(order.getId()); // 1 kitchen ticket

            orderService.addItems(order.getId(), List.of(new OrderItemInput(barItem.getId(), 1))); // triggers its own dispatch

            List<PrintJobEntity> jobs = jobsFor(order.getId());
            assertThat(jobs).hasSize(2);
            PrintJobEntity barJob =
                    jobs.stream().filter(j -> j.getPrinterId().equals(barPrinter.getId())).findFirst().orElseThrow();
            PrintJobEntity kitchenJob =
                    jobs.stream().filter(j -> j.getPrinterId().equals(kitchenPrinter.getId())).findFirst().orElseThrow();
            assertThat(decode(barJob)).contains("1x Mojito").doesNotContain("Caesar Salad");
            assertThat(decode(kitchenJob)).contains("1x Caesar Salad").doesNotContain("Mojito");
        }
    }

    // --- Fail-open: an unreachable printer never blocks the order operation ---

    @Test
    void sendOrder_printerUnreachable_orderStillTransitions_printJobEndsFailed() throws IOException {
        persistPrinter(PrinterDepartment.KITCHEN, unreachablePort());

        Order order = orderService.create(new OrderCreateInput(), cashier.getId());
        orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));

        Order sent = sendOrder(order.getId()); // must not throw despite the dead printer

        assertThat(sent.getStatus().getValue()).isEqualTo("SENT");
        List<PrintJobEntity> jobs = jobsFor(order.getId());
        assertThat(jobs).hasSize(1);
        PrintJobEntity job = jobs.get(0);
        assertThat(job.getStatus()).isEqualTo(PrintJobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getLastError()).isNotBlank();
    }

    // --- Manual retry recovers once the printer is reachable again ---

    @Test
    void retryPrintJob_afterPrinterComesBackOnline_succeeds() throws IOException {
        Printer printer = persistPrinterDto(PrinterDepartment.KITCHEN, unreachablePort());

        Order order = orderService.create(new OrderCreateInput(), cashier.getId());
        orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
        sendOrder(order.getId());

        PrintJobEntity failedJob = jobsFor(order.getId()).get(0);
        assertThat(failedJob.getStatus()).isEqualTo(PrintJobStatus.FAILED);

        try (FakePrinter fake = new FakePrinter()) {
            // The printer "comes back online" - point the same Printer row at a reachable host.
            PrinterInput reconnected = new PrinterInput(printer.getName(), PrinterDepartment.KITCHEN, "127.0.0.1");
            reconnected.setPort(fake.port());
            printerService.update(printer.getId(), reconnected);

            PrintJob retried = printerService.retryPrintJob(failedJob.getId(), Role.MANAGER);

            assertThat(retried.getStatus()).isEqualTo(PrintJobStatus.SENT);
            assertThat(retried.getAttempts()).isEqualTo(2); // 1 automatic + 1 manual
        }
    }

    // --- Z-report: same reconciliation numbers as the CSV export, ROOM_CHARGE excluded from received money ---

    @Test
    void closeShift_printsZReport_roomChargeExcludedFromReceivedTotal() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            persistPrinter(PrinterDepartment.CASHIER, fake.port());

            RoomEntity room = new RoomEntity();
            room.setName("Ocean View Suite");
            room.setDescription("A room used only by tests");
            room.setCapacity(2);
            room.setBasePrice(new BigDecimal("1000.00"));
            room = roomRepository.saveAndFlush(room);

            BookingEntity booking = new BookingEntity();
            booking.setRoomId(room.getId());
            booking.setGuestName("Jane Doe");
            booking.setGuestEmail("jane@example.com");
            booking.setGuestPhone("+66800000000");
            booking.setCheckIn(LocalDate.now());
            booking.setCheckOut(LocalDate.now().plusDays(1));
            booking.setTotalPrice(new BigDecimal("1000.00"));
            booking.setStatus(BookingStatus.CONFIRMED);
            booking = bookingRepository.saveAndFlush(booking);
            // Forces OrderService.close()'s later bookingRepository.findById() to hydrate a fresh
            // lazy Room proxy instead of reusing this already-managed instance, whose `room`
            // association was never populated - see BookingRoomChargeTests.persistBooking().
            entityManager.clear();

            Shift shift = shiftService.open(cashier.getId(), new ShiftOpenInput().openingCashFloat(new BigDecimal("1000.00")));

            Order cashOrder = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(cashOrder.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 2))); // 200.00
            orderService.close(cashOrder.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

            Order cardOrder = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(cardOrder.getId(), List.of(new OrderItemInput(barItem.getId(), 3))); // 300.00
            orderService.close(cardOrder.getId(), new CloseOrderInput(PaymentMethod.CARD), cashier.getId());

            MenuItemEntity suiteDinner = persistMenuItem("Suite Dinner", MenuDepartment.KITCHEN, "5000.00");
            Order roomOrder = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(roomOrder.getId(), List.of(new OrderItemInput(suiteDinner.getId(), 1))); // 5000.00
            orderService.close(
                    roomOrder.getId(),
                    new CloseOrderInput(PaymentMethod.ROOM_CHARGE).bookingId(booking.getId()),
                    cashier.getId());

            shiftService.close(shift.getId(), new ShiftCloseInput().closingCashCounted(new BigDecimal("1200.00")));

            PrintJobEntity job = printJobRepository.findAll().stream()
                    .filter(j -> j.getDocumentType() == PrintDocumentType.Z_REPORT)
                    .filter(j -> j.getSummary().contains(shift.getId().substring(0, 8).toUpperCase()))
                    .findFirst()
                    .orElseThrow();

            String text = decode(job);
            assertThat(lineFor(text, "Cash")).endsWith("200.00");
            assertThat(lineFor(text, "Card")).endsWith("300.00");
            assertThat(lineFor(text, "Room charge")).endsWith("5000.00");
            // Received total = cash + card only (500.00), never cash + card + room charge (5500.00).
            assertThat(lineFor(text, "Received total")).endsWith("500.00");
            assertThat(lineFor(text, "Opening float")).endsWith("1000.00");
            assertThat(lineFor(text, "Expected cash")).endsWith("1200.00"); // 1000 float + 200 cash, not the room charge
            assertThat(lineFor(text, "Counted cash")).endsWith("1200.00");
            assertThat(lineFor(text, "Discrepancy")).endsWith("0.00");
        }
    }

    // --- Print-job queue visibility is filtered by caller role, not just gated at the security layer ---

    @Test
    void listPrintJobs_waiterRole_seesKitchenAndBarTicketsNotZReportOrGuestReceipt_andSummariesCarryNoMoney() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            persistPrinter(PrinterDepartment.KITCHEN, fake.port());
            persistPrinter(PrinterDepartment.BAR, fake.port());
            persistPrinter(PrinterDepartment.CASHIER, fake.port());

            Order ticketOrder = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(
                    ticketOrder.getId(),
                    List.of(new OrderItemInput(kitchenItem.getId(), 1), new OrderItemInput(barItem.getId(), 1)));
            sendOrder(ticketOrder.getId()); // -> KITCHEN_TICKET + BAR_TICKET
            // ShiftService.close()'s unsettled-order guard is system-wide (see
            // OrderCloseAndShiftGuardTests) - a SENT order anywhere blocks any shift from
            // closing, so this one must be settled before the shift-close call below.
            orderService.cancel(ticketOrder.getId());

            Shift shift = shiftService.open(cashier.getId(), new ShiftOpenInput());
            Order paidOrder = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(paidOrder.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            orderService.close(paidOrder.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId()); // -> GUEST_RECEIPT
            shiftService.close(shift.getId(), new ShiftCloseInput()); // -> Z_REPORT

            // The dev DB this suite runs against may already carry other jobs (e.g. staff manually
            // trying the feature), so these assert set membership, not exact composition -
            // KITCHEN_TICKET/BAR_TICKET/PREBILL can legitimately be visible from elsewhere;
            // Z_REPORT/GUEST_RECEIPT must never be, no matter what else is in the table.
            List<PrintJob> waiterView = printerService.listPrintJobs(null, null, Role.WAITER);
            assertThat(waiterView)
                    .extracting(PrintJob::getDocumentType)
                    .contains(PrintDocumentType.KITCHEN_TICKET, PrintDocumentType.BAR_TICKET);
            assertThat(waiterView)
                    .extracting(PrintJob::getDocumentType)
                    .doesNotContain(PrintDocumentType.Z_REPORT, PrintDocumentType.GUEST_RECEIPT, PrintDocumentType.TEST_PAGE);
            // Same filtering for CASHIER - only MANAGER/ADMIN get the cashier-facing documents.
            List<PrintJob> cashierView = printerService.listPrintJobs(null, null, Role.CASHIER);
            assertThat(cashierView)
                    .extracting(PrintJob::getDocumentType)
                    .doesNotContain(PrintDocumentType.Z_REPORT, PrintDocumentType.GUEST_RECEIPT, PrintDocumentType.TEST_PAGE);
            // No money in what a waiter is allowed to see - a kitchen/bar ticket's summary is just "what and where".
            assertThat(waiterView).extracting(PrintJob::getSummary).noneMatch(s -> s.matches(".*\\d+\\.\\d{2}.*"));

            List<PrintJob> managerView = printerService.listPrintJobs(null, null, Role.MANAGER);
            assertThat(managerView)
                    .extracting(PrintJob::getDocumentType)
                    .contains(
                            PrintDocumentType.KITCHEN_TICKET,
                            PrintDocumentType.BAR_TICKET,
                            PrintDocumentType.GUEST_RECEIPT,
                            PrintDocumentType.Z_REPORT);

            // ?documentType= actually filters, not just ?status=.
            List<PrintJob> zReportsOnly = printerService.listPrintJobs(null, PrintDocumentType.Z_REPORT, Role.MANAGER);
            assertThat(zReportsOnly).extracting(PrintJob::getDocumentType).containsOnly(PrintDocumentType.Z_REPORT);

            // ?documentType=BAR_TICKET returns bar tickets only - not the kitchen ticket from the same order.
            List<PrintJob> barTicketsOnly = printerService.listPrintJobs(null, PrintDocumentType.BAR_TICKET, Role.WAITER);
            assertThat(barTicketsOnly).extracting(PrintJob::getDocumentType).containsOnly(PrintDocumentType.BAR_TICKET);

            // A CASH guest receipt's summary names the payment method - distinguishable from a room-charge one at a glance.
            PrintJob guestReceipt = managerView.stream()
                    .filter(j -> j.getDocumentType() == PrintDocumentType.GUEST_RECEIPT)
                    .findFirst()
                    .orElseThrow();
            assertThat(guestReceipt.getSummary()).contains("(CASH)");
        }
    }

    @Test
    void printGuestReceipt_roomCharge_summaryNamesGuestNotAmount() throws IOException {
        try (FakePrinter fake = new FakePrinter()) {
            persistPrinter(PrinterDepartment.CASHIER, fake.port());

            RoomEntity room = new RoomEntity();
            room.setName("Ocean View Suite");
            room.setDescription("A room used only by tests");
            room.setCapacity(2);
            room.setBasePrice(new BigDecimal("1000.00"));
            room = roomRepository.saveAndFlush(room);

            BookingEntity booking = new BookingEntity();
            booking.setRoomId(room.getId());
            booking.setGuestName("Jane Doe");
            booking.setGuestEmail("jane@example.com");
            booking.setGuestPhone("+66800000000");
            booking.setCheckIn(LocalDate.now());
            booking.setCheckOut(LocalDate.now().plusDays(1));
            booking.setTotalPrice(new BigDecimal("1000.00"));
            booking.setStatus(BookingStatus.CONFIRMED);
            booking = bookingRepository.saveAndFlush(booking);
            entityManager.clear(); // see closeShift_printsZReport_... for why

            Order order = orderService.create(new OrderCreateInput(), cashier.getId());
            orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 1)));
            shiftService.open(cashier.getId(), new ShiftOpenInput());
            orderService.close(
                    order.getId(), new CloseOrderInput(PaymentMethod.ROOM_CHARGE).bookingId(booking.getId()), cashier.getId());

            PrintJob receipt = printerService.listPrintJobs(null, PrintDocumentType.GUEST_RECEIPT, Role.MANAGER).stream()
                    .findFirst()
                    .orElseThrow();
            assertThat(receipt.getSummary()).contains("ROOM_CHARGE").contains("Jane Doe");
            assertThat(receipt.getSummary()).doesNotMatch(".*\\d+\\.\\d{2}.*"); // no amount, just method + guest
        }
    }

    // --- Preview: same text a printer would render, ESC/POS control bytes stripped, role-gated like retry ---

    @Test
    void previewPrintJob_visibleType_returnsReadableTextWithoutEscPosBytes() throws IOException {
        persistPrinter(PrinterDepartment.KITCHEN, unreachablePort());

        Order order = orderService.create(new OrderCreateInput(), cashier.getId());
        orderService.addItems(order.getId(), List.of(new OrderItemInput(kitchenItem.getId(), 2)));
        sendOrder(order.getId());

        PrintJobEntity job = jobsFor(order.getId()).get(0);

        String preview = printerService.previewPrintJob(job.getId(), Role.WAITER);

        assertThat(preview).contains("KITCHEN TICKET");
        assertThat(preview).contains("2x Caesar Salad");
        // No stray ESC (0x1B) / GS (0x1D) control bytes left in what's returned.
        assertThat(preview.chars()).noneMatch(c -> c == 0x1B || c == 0x1D);
    }

    @Test
    void previewPrintJob_waiterRole_hiddenDocumentType_throwsNotFound() throws IOException {
        PrinterEntity printer = persistPrinter(PrinterDepartment.CASHIER, unreachablePort());
        PrintJob testPageJob = printerService.testPrint(printer.getId()); // TEST_PAGE - not staff-visible

        assertThatThrownBy(() -> printerService.previewPrintJob(testPageJob.getId(), Role.WAITER))
                .isInstanceOf(NotFoundException.class);

        // MANAGER can still see it.
        assertThat(printerService.previewPrintJob(testPageJob.getId(), Role.MANAGER)).contains("TEST PAGE");
    }

    @Test
    void retryPrintJob_waiterRole_documentTypeNotVisible_throwsNotFound_managerCanStillRetryIt() throws IOException {
        PrinterEntity printer = persistPrinter(PrinterDepartment.CASHIER, unreachablePort());
        PrintJob testPageJob = printerService.testPrint(printer.getId()); // TEST_PAGE - not staff-visible

        assertThatThrownBy(() -> printerService.retryPrintJob(testPageJob.getId(), Role.WAITER))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> printerService.retryPrintJob(testPageJob.getId(), Role.CASHIER))
                .isInstanceOf(NotFoundException.class);

        PrintJob retried = printerService.retryPrintJob(testPageJob.getId(), Role.MANAGER);
        assertThat(retried.getId()).isEqualTo(testPageJob.getId());
    }

    // --- helpers ---

    private MenuItemEntity persistMenuItem(String name, MenuDepartment department, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name);
        item.setDescription("test item");
        item.setCategory("Test");
        item.setDepartment(department);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        return menuItemRepository.saveAndFlush(item);
    }

    private PrinterEntity persistPrinter(PrinterDepartment department, int port) {
        return printerRepository.findById(persistPrinterDto(department, port).getId()).orElseThrow();
    }

    /**
     * The dev DB this suite runs against is shared with manual/staff testing of the feature, and
     * {@code Printer_one_active_per_department} allows only one active printer per department -
     * so if staff already registered a real one (e.g. while trying the feature out), creating a
     * second active KITCHEN/BAR/CASHIER printer 409s. Repointing the existing row's host/port
     * instead is safe: this method only ever runs inside a {@code @Transactional} test, so the
     * repoint is rolled back with everything else at the end of the test, restoring whatever was
     * configured before.
     */
    private Printer persistPrinterDto(PrinterDepartment department, int port) {
        PrinterInput input = new PrinterInput("Test " + department + " printer " + UUID.randomUUID(), department, "127.0.0.1");
        input.setPort(port);
        return printerRepository
                .findByDepartmentAndIsActiveTrue(department)
                .map(existing -> printerService.update(existing.getId(), input))
                .orElseGet(() -> printerService.create(input));
    }

    private Order sendOrder(String orderId) {
        return orderService.update(orderId, new OrderUpdateInput().status(com.sunsetbeach.model.OrderStatus.SENT));
    }

    private List<PrintJobEntity> jobsFor(String orderId) {
        // PrintJob doesn't carry orderId directly - every summary generated for an order embeds
        // its short id (see OrderPrintingService.shortId), which is unique enough to filter on here.
        String shortId = orderId.substring(0, 8).toUpperCase();
        return printJobRepository.findAll().stream()
                .filter(j -> j.getSummary().contains(shortId))
                .toList();
    }

    private static String decode(PrintJobEntity job) {
        return new String(job.getPayload(), PrinterCodepages.charset(PrinterCodepage.PC437));
    }

    /** The two-column line whose trimmed content starts with {@code label} (e.g. "Cash", "Discrepancy"). */
    private static String lineFor(String text, String label) {
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(l -> l.startsWith(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line starting with \"" + label + "\" in:\n" + text));
    }

    /** A free loopback TCP port with nothing listening on it - connecting fails fast with "connection refused". */
    private static int unreachablePort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            return probe.getLocalPort();
        }
    }

    /** A minimal always-up "printer": accepts TCP connections on loopback and discards whatever is written. */
    private static final class FakePrinter implements AutoCloseable {
        private final ServerSocket server;
        private final Thread acceptThread;

        FakePrinter() throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            acceptThread = new Thread(this::acceptLoop, "fake-printer-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        int port() {
            return server.getLocalPort();
        }

        private void acceptLoop() {
            while (!server.isClosed()) {
                try {
                    Socket socket = server.accept();
                    Thread drain = new Thread(() -> drain(socket), "fake-printer-drain");
                    drain.setDaemon(true);
                    drain.start();
                } catch (IOException e) {
                    break;
                }
            }
        }

        private static void drain(Socket socket) {
            try (socket) {
                socket.getInputStream().readAllBytes();
            } catch (IOException ignored) {
                // Connection torn down - nothing left to drain.
            }
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
