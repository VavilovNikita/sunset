package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.FolioPayment;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /reports/revenue-export against a real scenario: a room-charged POS order that is later
 * settled at the desk must count exactly once - through its FolioPayment, never through its
 * ROOM_CHARGE Payment - while that Payment stays visible on the detail sheet. Also covers the
 * Asia/Bangkok day boundary (the whole reason this report does not simply reuse
 * PaymentService#getSummary's window) and that the PAID-bookings info sheet stays out of every total.
 *
 * <p>{@code @Transactional} (rolled back); the audit rows AuditLogService writes in their own
 * REQUIRES_NEW transaction are cleaned up in {@link #cleanUpAuditRows}. Fixtures use a far-future
 * window, backdated with a native UPDATE (same approach as PaymentsSummaryTests, which uses 2031),
 * with every createdAt written as UTC wall-clock - what @CreationTimestamp stores in production.
 */
@SpringBootTest
@Transactional
class RevenueExportServiceTests extends AbstractIntegrationTest {

    private static final LocalDate FROM = LocalDate.of(2032, 4, 10);
    private static final LocalDate TO = LocalDate.of(2032, 4, 12);
    /** Bangkok midnight at the start of FROM, and after TO, as UTC wall-clock (UTC+7, no DST). */
    private static final LocalDateTime WINDOW_START_UTC = LocalDateTime.of(2032, 4, 9, 17, 0);
    private static final LocalDateTime WINDOW_END_UTC = LocalDateTime.of(2032, 4, 12, 17, 0);

    // Summary sheet column indexes - see RevenueExportService's SUMMARY_COLUMNS.
    private static final int POS_CASH = 1, POS_CARD = 2, POS_OTHER = 3, POS_RECEIVED = 4;
    private static final int FOLIO_CASH = 5, FOLIO_TOTAL = 8, TOTAL_COLLECTED = 9, ROOM_CHARGED = 10;

    @Autowired private RevenueExportService revenueExportService;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private EntityManager entityManager;

    private UserEntity user;
    private ShiftEntity shift;
    private RoomEntity room;
    private TableEntity table;
    private final List<String> bookingIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        UserEntity newUser = new UserEntity();
        newUser.setEmail("revenue-export-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.MANAGER);
        user = userRepository.saveAndFlush(newUser);

        ShiftEntity newShift = new ShiftEntity();
        newShift.setOpenedByUserId(user.getId());
        newShift.setStatus(ShiftStatus.CLOSED);
        shift = shiftRepository.saveAndFlush(newShift);

        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Revenue Export Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by RevenueExportServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1500.00"));
        room = roomRepository.saveAndFlush(newRoom);

        TableEntity newTable = new TableEntity();
        newTable.setZone(Zone.RESTAURANT);
        newTable.setLabel("RX-" + UUID.randomUUID().toString().substring(0, 8));
        newTable.setCapacity(2);
        newTable.setActive(true);
        table = tableRepository.saveAndFlush(newTable);

        // recordFolioPayment and AuditLogService both read the acting user off the security context.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(user.getId(), user.getEmail(), Role.MANAGER), null, List.of()));
    }

    @AfterEach
    void cleanUpAuditRows() {
        SecurityContextHolder.clearContext();
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> user.getEmail().equals(e.getActorEmail()))
                .filter(e -> e.getAction() == AuditAction.REVENUE_EXPORTED
                        || (e.getAction() == AuditAction.BOOKING_FOLIO_PAYMENT_RECORDED && bookingIds.contains(e.getEntityId())))
                .toList());
    }

    private BookingEntity persistBooking(BookingStatus status, LocalDate checkIn, LocalDate checkOut, String guestName, String totalPrice) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName(guestName);
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(new BigDecimal(totalPrice));
        booking.setStatus(status);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        bookingIds.add(saved.getId());
        return saved;
    }

    private PaymentEntity persistPayment(PaymentMethod method, String amount, LocalDateTime createdAtUtc, TableEntity atTable, BookingEntity chargedTo) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(user.getId());
        if (atTable != null) order.setTableId(atTable.getId());
        order = orderRepository.saveAndFlush(order);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setAmount(new BigDecimal(amount));
        payment.setRecordedByUserId(user.getId());
        payment.setShiftId(shift.getId());
        if (chargedTo != null) payment.setBookingId(chargedTo.getId());
        payment = paymentRepository.saveAndFlush(payment);
        backdate("Payment", payment.getId(), createdAtUtc);
        return payment;
    }

    private void backdate(String table, String id, LocalDateTime createdAtUtc) {
        entityManager.createNativeQuery("UPDATE \"" + table + "\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, createdAtUtc)
                .setParameter(2, id)
                .executeUpdate();
        entityManager.clear();
    }

    private static XSSFWorkbook open(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private static Row findRow(Sheet sheet, String firstCell) {
        for (Row row : sheet) {
            if (row.getCell(0) != null && firstCell.equals(row.getCell(0).toString())) return row;
        }
        throw new AssertionError("No row starting with " + firstCell + " on sheet " + sheet.getSheetName());
    }

    private static BigDecimal amount(Row row, int column) {
        return BigDecimal.valueOf(row.getCell(column).getNumericCellValue()).setScale(2);
    }

    private BigDecimal independentlyComputedCollected() {
        Object pos = entityManager.createNativeQuery(
                        "SELECT COALESCE(SUM(amount), 0) FROM \"Payment\" WHERE method <> 'ROOM_CHARGE' AND \"createdAt\" >= ?1 AND \"createdAt\" < ?2")
                .setParameter(1, WINDOW_START_UTC).setParameter(2, WINDOW_END_UTC).getSingleResult();
        Object folio = entityManager.createNativeQuery(
                        "SELECT COALESCE(SUM(amount), 0) FROM \"FolioPayment\" WHERE \"createdAt\" >= ?1 AND \"createdAt\" < ?2")
                .setParameter(1, WINDOW_START_UTC).setParameter(2, WINDOW_END_UTC).getSingleResult();
        return new BigDecimal(pos.toString()).add(new BigDecimal(folio.toString())).setScale(2);
    }

    @Test
    void roomChargedOrderSettledInPeriod_countsOnceThroughItsFolioPayment() throws IOException {
        BookingEntity booking = persistBooking(BookingStatus.PAID, FROM.minusDays(1), FROM.plusDays(1), "Revenue Guest", "3000.00");

        // 06:00 Bangkok on the 10th = 23:00 UTC on the 9th - inside the Bangkok window only.
        PaymentEntity cash = persistPayment(PaymentMethod.CASH, "200.00", LocalDateTime.of(2032, 4, 9, 23, 0), table, null);
        // Dinner closed to the room at 12:00 Bangkok on the 10th - moved to the folio, nothing collected.
        PaymentEntity roomCharge = persistPayment(PaymentMethod.ROOM_CHARGE, "450.00", LocalDateTime.of(2032, 4, 10, 5, 0), null, booking);
        // Collected at checkout through the real path (it rejects anything above outstanding room charges).
        FolioPayment settlement = bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "450.00"));
        backdate("FolioPayment", settlement.getId(), LocalDateTime.of(2032, 4, 11, 3, 0));
        // 01:30 Bangkok on the 12th = 18:30 UTC on the 11th - a Bangkok-day report puts it on the 12th.
        persistPayment(PaymentMethod.CARD, "300.00", LocalDateTime.of(2032, 4, 11, 18, 30), table, null);
        persistPayment(PaymentMethod.OTHER, "25.50", LocalDateTime.of(2032, 4, 12, 16, 59), null, null);
        // Just outside, on both ends, in Bangkok terms.
        persistPayment(PaymentMethod.CASH, "888.00", LocalDateTime.of(2032, 4, 9, 16, 30), null, null);
        persistPayment(PaymentMethod.CASH, "999.00", LocalDateTime.of(2032, 4, 12, 17, 30), null, null);

        try (XSSFWorkbook workbook = open(revenueExportService.export(FROM.toString(), TO.toString()))) {
            Sheet summary = workbook.getSheet("Summary");
            Row total = findRow(summary, "Total");

            assertThat(amount(total, POS_CASH)).isEqualByComparingTo("200.00");
            assertThat(amount(total, POS_CARD)).isEqualByComparingTo("300.00");
            assertThat(amount(total, POS_OTHER)).isEqualByComparingTo("25.50");
            assertThat(amount(total, POS_RECEIVED)).isEqualByComparingTo("525.50");
            // The room charge is shown, in full, but is not in POS received...
            assertThat(amount(total, ROOM_CHARGED)).isEqualByComparingTo("450.00");
            // ...and its money counts only once collected, through the folio payment.
            assertThat(amount(total, FOLIO_CASH)).isEqualByComparingTo("450.00");
            assertThat(amount(total, FOLIO_TOTAL)).isEqualByComparingTo("450.00");
            // Room rate (3000.00 on a PAID booking) is in no total.
            assertThat(amount(total, TOTAL_COLLECTED)).isEqualByComparingTo("975.50");
            assertThat(amount(total, TOTAL_COLLECTED)).isEqualByComparingTo(independentlyComputedCollected());

            // Bangkok days: 06:00 on the 10th lands on the 10th, 01:30 on the 12th on the 12th.
            assertThat(amount(findRow(summary, "2032-04-10"), TOTAL_COLLECTED)).isEqualByComparingTo("200.00");
            assertThat(amount(findRow(summary, "2032-04-10"), ROOM_CHARGED)).isEqualByComparingTo("450.00");
            assertThat(amount(findRow(summary, "2032-04-11"), TOTAL_COLLECTED)).isEqualByComparingTo("450.00");
            assertThat(amount(findRow(summary, "2032-04-12"), TOTAL_COLLECTED)).isEqualByComparingTo("325.50");

            Sheet pos = workbook.getSheet("POS payments");
            assertThat(pos.getLastRowNum()).isEqualTo(4);
            Row roomChargeRow = findRowWithCell(pos, 5, roomCharge.getOrderId());
            assertThat(roomChargeRow.getCell(2).toString()).isEqualTo("ROOM_CHARGE");
            assertThat(roomChargeRow.getCell(4).toString()).startsWith("No");
            assertThat(roomChargeRow.getCell(7).toString()).isEqualTo(booking.getId());
            Row cashRow = findRowWithCell(pos, 5, cash.getOrderId());
            assertThat(cashRow.getCell(0).toString()).isEqualTo("2032-04-10");
            assertThat(cashRow.getCell(1).toString()).isEqualTo("06:00");
            assertThat(cashRow.getCell(4).toString()).isEqualTo("Yes");
            assertThat(cashRow.getCell(6).toString()).isEqualTo(table.getLabel());

            Sheet folio = workbook.getSheet("Room charge settlements");
            assertThat(folio.getLastRowNum()).isEqualTo(1);
            Row settlementRow = folio.getRow(1);
            assertThat(settlementRow.getCell(0).toString()).isEqualTo("2032-04-11");
            assertThat(settlementRow.getCell(2).toString()).isEqualTo("CASH");
            assertThat(amount(settlementRow, 3)).isEqualByComparingTo("450.00");
            assertThat(settlementRow.getCell(4).toString()).isEqualTo(booking.getId());
            assertThat(settlementRow.getCell(5).toString()).isEqualTo("Revenue Guest");

            Sheet paid = workbook.getSheet("Paid bookings (info)");
            Row paidRow = findRow(paid, booking.getId());
            assertThat(amount(paidRow, 4)).isEqualByComparingTo("3000.00");
        }

        assertThat(auditLogRepository.findAll()).anySatisfy(e -> {
            assertThat(e.getAction()).isEqualTo(AuditAction.REVENUE_EXPORTED);
            assertThat(e.getActorEmail()).isEqualTo(user.getEmail());
        });
    }

    @Test
    void roomChargedButNotYetSettled_isCollectedNowhere() throws IOException {
        BookingEntity booking = persistBooking(BookingStatus.CONFIRMED, FROM, TO.plusDays(3), "Still In House", "5000.00");
        persistPayment(PaymentMethod.ROOM_CHARGE, "700.00", LocalDateTime.of(2032, 4, 10, 5, 0), null, booking);

        try (XSSFWorkbook workbook = open(revenueExportService.export(FROM.toString(), TO.toString()))) {
            Row total = findRow(workbook.getSheet("Summary"), "Total");
            assertThat(amount(total, ROOM_CHARGED)).isEqualByComparingTo("700.00");
            assertThat(amount(total, TOTAL_COLLECTED)).isEqualByComparingTo("0.00");
            // Not PAID, so not on the info sheet either (header note + blank + header = 3 rows).
            assertThat(workbook.getSheet("Paid bookings (info)").getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void emptyPeriod_hasOneZeroRowPerDay() throws IOException {
        try (XSSFWorkbook workbook = open(revenueExportService.export(FROM.toString(), TO.toString()))) {
            Sheet summary = workbook.getSheet("Summary");
            for (LocalDate d = FROM; !d.isAfter(TO); d = d.plusDays(1)) {
                assertThat(amount(findRow(summary, d.toString()), TOTAL_COLLECTED)).isEqualByComparingTo("0.00");
            }
            assertThat(amount(findRow(summary, "Total"), TOTAL_COLLECTED)).isEqualByComparingTo("0.00");
        }
    }

    @Test
    void fromAfterTo_isRejected() {
        assertThatThrownBy(() -> revenueExportService.export(TO.toString(), FROM.toString())).isInstanceOf(ValidationException.class);
    }

    @Test
    void impossibleDateMatchingThePattern_isRejectedNotA500() {
        assertThatThrownBy(() -> revenueExportService.export("2032-02-30", "2032-03-01")).isInstanceOf(ValidationException.class);
    }

    private static Row findRowWithCell(Sheet sheet, int column, String value) {
        for (Row row : sheet) {
            if (row.getCell(column) != null && value.equals(row.getCell(column).toString())) return row;
        }
        throw new AssertionError("No row with " + value + " in column " + column + " on sheet " + sheet.getSheetName());
    }
}
