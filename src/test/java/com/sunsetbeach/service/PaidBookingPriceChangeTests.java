package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationQuote;
import com.sunsetbeach.model.RelocationRateMode;
import com.sunsetbeach.model.RepriceInput;
import com.sunsetbeach.model.RepriceQuote;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.SettlementStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A change of price on a booking marked PAID reopens it (owner's decision, A): {@code settledOutside} must never swallow
 * the growth. Covers the three write paths (relocation, schedule change, reprice), the ledger (the books agree with the
 * folio after the difference is paid) and a legacy PAID row whose room was settled outside the system. "Today" is
 * 2034-03-10. Not {@code @Transactional}: the reopening runs in its own transaction after the writer's.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
class PaidBookingPriceChangeTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LedgerService ledgerService;
    @Autowired private JdbcTemplate jdbc;

    private final List<String> roomIds = new ArrayList<>();
    private final List<String> bookingIds = new ArrayList<>();
    private String userId;

    @BeforeEach
    void signIn() {
        UserEntity user = new UserEntity();
        user.setEmail("paid-change-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.MANAGER);
        userId = userRepository.saveAndFlush(user).getId();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new StaffPrincipal(userId, user.getEmail(), Role.MANAGER), null, List.of()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String bookingId : bookingIds) {
            List<String> sources = new ArrayList<>(jdbc.queryForList("SELECT id FROM \"FolioPayment\" WHERE \"bookingId\" = ?", String.class, bookingId));
            sources.add(bookingId);
            String in = sources.stream().map(x -> "?").collect(Collectors.joining(","));
            jdbc.update("DELETE FROM \"JournalLine\" WHERE \"entryId\" IN (SELECT id FROM \"JournalEntry\" WHERE \"sourceId\" IN (" + in + "))", sources.toArray());
            jdbc.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" IN (" + in + ")", sources.toArray());
            jdbc.update("DELETE FROM \"FolioPayment\" WHERE \"bookingId\" = ?", bookingId);
            jdbc.update("DELETE FROM \"AuditLog\" WHERE \"entityId\" = ?", bookingId);
        }
        List<String> segmentBookings = segmentRepository.findAll().stream()
                .filter(s -> roomIds.contains(s.getRoomId()))
                .map(BookingSegmentEntity::getBookingId)
                .distinct()
                .toList();
        if (!segmentBookings.isEmpty()) {
            bookingRepository.deleteAllById(segmentBookings);
        }
        for (String roomId : roomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        roomIds.clear();
        bookingIds.clear();
        userRepository.deleteById(userId);
    }

    private RoomEntity room(String basePrice) {
        RoomEntity r = new RoomEntity();
        r.setName("PaidChange Room " + UUID.randomUUID());
        r.setDescription("Used only by PaidBookingPriceChangeTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal(basePrice));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        roomIds.add(saved.getId());
        for (int i = 0; i < 2; i++) {
            RoomUnitEntity u = new RoomUnitEntity();
            u.setRoomId(saved.getId());
            u.setLabel("PC-" + UUID.randomUUID().toString().substring(0, 8));
            u.setActive(true);
            u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
            roomUnitRepository.saveAndFlush(u);
        }
        return saved;
    }

    private RoomUnitEntity unit(RoomEntity room, int index) {
        return roomUnitRepository.findByRoomId(room.getId()).stream()
                .sorted(java.util.Comparator.comparing(RoomUnitEntity::getLabel))
                .toList()
                .get(index);
    }

    private Booking booking(RoomEntity room, RoomUnitEntity unit, LocalDate checkIn, int nights) {
        Booking b = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "Guest " + UUID.randomUUID(), checkIn.toString(), checkIn.plusDays(nights).toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        bookingIds.add(b.getId());
        return b;
    }

    /** Marks the room paid the way the desk does now: the payment travels with the status change. */
    private void markPaid(Booking b) {
        bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.PAID)
                .payment(new FolioPaymentInput(FolioPaymentMethod.CASH, b.getTotalPrice())));
        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
    }

    private BookingStatus status(Booking b) {
        return bookingRepository.findById(b.getId()).orElseThrow().getStatus();
    }

    private BookingFolio folio(Booking b) {
        return bookingService.getFolio(b.getId());
    }

    private RelocationInput move(LocalDate from, RoomEntity room, RoomUnitEntity unit, RelocationRateMode mode) {
        RelocationInput in = new RelocationInput(from.toString(), room.getId()).roomUnitId(unit.getId());
        in.setRateMode(mode);
        return in;
    }

    private static void assertMoney(String actual, String expected) {
        assertThat(new BigDecimal(actual)).isEqualByComparingTo(expected);
    }

    /** Debit minus credit per ledger account over everything this booking posted (its settlement and its folio payments). */
    private Map<String, BigDecimal> ledger(Booking b) {
        List<String> sources = new ArrayList<>(jdbc.queryForList("SELECT id FROM \"FolioPayment\" WHERE \"bookingId\" = ?", String.class, b.getId()));
        sources.add(b.getId());
        String in = sources.stream().map(x -> "?").collect(Collectors.joining(","));
        Map<String, BigDecimal> net = new HashMap<>();
        jdbc.query(
                "SELECT l.\"accountCode\" AS code, sum(l.debit) - sum(l.credit) AS net FROM \"JournalLine\" l "
                        + "JOIN \"JournalEntry\" e ON e.id = l.\"entryId\" WHERE e.\"sourceId\" IN (" + in + ") GROUP BY l.\"accountCode\"",
                rs -> {
                    net.put(rs.getString("code"), rs.getBigDecimal("net"));
                },
                sources.toArray());
        return net;
    }

    private BigDecimal net(Map<String, BigDecimal> ledger, String account) {
        return ledger.getOrDefault(account, BigDecimal.ZERO);
    }

    /** Room revenue as the books hold it: revenue and VAT payable are both credits for the gross collected. */
    private BigDecimal revenueGross(Map<String, BigDecimal> ledger) {
        return net(ledger, "4000").add(net(ledger, "2100")).negate();
    }

    // --- Relocation ---------------------------------------------------------------------------

    @Test
    void relocationThatRaisesThePrice_reopensTheBooking_andTheFolioOwesTheDifference() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3); // 3 000
        markPaid(b);

        bookingService.relocate(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(status(b)).isEqualTo(BookingStatus.CONFIRMED);
        BookingFolio f = folio(b);
        assertMoney(f.getSettledOutside(), "0");
        assertMoney(f.getBalanceDue(), "6000");
        assertMoney(f.getPaidTotal(), "3000");
    }

    @Test
    void afterTheDifferenceIsPaid_theBookingIsPaidAgain_andTheBooksMatchTheFolio() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        markPaid(b);
        bookingService.relocate(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        // While it is reopened the books hold no room revenue - only the 3 000 the guest actually paid.
        Map<String, BigDecimal> open = ledger(b);
        assertThat(revenueGross(open)).isEqualByComparingTo("0");
        assertThat(net(open, "1000")).isEqualByComparingTo("3000");

        bookingService.recordFolioPayment(b.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "6000.00"));

        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
        BookingFolio f = folio(b);
        assertMoney(f.getBalanceDue(), "0");
        assertMoney(f.getSettledOutside(), "0");
        Map<String, BigDecimal> settled = ledger(b);
        assertThat(revenueGross(settled)).isEqualByComparingTo("9000"); // = the folio's room total
        assertThat(net(settled, "1000")).isEqualByComparingTo("9000"); // all the cash collected
        assertThat(net(settled, "1100")).isEqualByComparingTo("0");    // nothing left on the guest ledger
        assertThat(ledgerService.trialBalance(TODAY.toString()).getBalanced()).isTrue();
    }

    @Test
    void keepRateRelocation_changesNoPrice_soAPaidBookingStaysPaid() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        markPaid(b);

        bookingService.relocate(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.KEEP_RATE), true, false);

        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
        assertMoney(folio(b).getBalanceDue(), "0");
    }

    @Test
    void theRelocationQuote_warnsBeforeConfirming_forAPaidBooking() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        markPaid(b);

        RelocationQuote quote = bookingService.quoteRelocation(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(quote.getReopensPaid()).isTrue();
        assertThat(quote.getPaidWarning()).isEqualTo("Room paid will be reopened: the guest will owe ฿6,000.");
        assertThat(status(b)).isEqualTo(BookingStatus.PAID); // a quote changes nothing
    }

    @Test
    void theQuoteSaysNothing_forABookingThatIsNotPaid() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);

        RelocationQuote quote = bookingService.quoteRelocation(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(quote.getReopensPaid()).isNull();
        assertThat(quote.getPaidWarning()).isNull();
    }

    // --- Schedule change ----------------------------------------------------------------------

    @Test
    void extendingAPaidStay_reopensIt_andTheNewNightIsOwed() {
        RoomEntity standard = room("1000.00");
        RoomUnitEntity u = unit(standard, 0);
        Booking b = booking(standard, u, TODAY.plusDays(5), 3);
        markPaid(b);
        BookingScheduleInput extend = new BookingScheduleInput(TODAY.plusDays(5).toString(), TODAY.plusDays(9).toString()).roomUnitId(u.getId());

        BookingScheduleQuote quote = bookingService.quoteSchedule(b.getId(), extend);
        assertThat(quote.getReopensPaid()).isTrue();
        assertThat(quote.getPaidWarning()).contains("Room paid will be reopened").contains("฿1,000");

        bookingService.updateSchedule(b.getId(), extend);

        assertThat(status(b)).isEqualTo(BookingStatus.CONFIRMED);
        BookingFolio f = folio(b);
        assertMoney(f.getBalanceDue(), "1000");
        assertMoney(f.getSettledOutside(), "0");
        assertThat(folioSettlement(b)).isEqualTo(SettlementStatus.OWES);
    }

    @Test
    void shorteningAPaidStay_staysPaid_withACredit_andTheBooksFollowTheNewPrice() {
        RoomEntity standard = room("1000.00");
        RoomUnitEntity u = unit(standard, 0);
        Booking b = booking(standard, u, TODAY.plusDays(5), 3);
        markPaid(b);
        BookingScheduleInput shorter = new BookingScheduleInput(TODAY.plusDays(5).toString(), TODAY.plusDays(7).toString()).roomUnitId(u.getId());

        BookingScheduleQuote quote = bookingService.quoteSchedule(b.getId(), shorter);
        assertThat(quote.getReopensPaid()).isFalse();
        assertThat(quote.getPaidWarning()).contains("credit of ฿1,000");

        bookingService.updateSchedule(b.getId(), shorter);

        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
        BookingFolio f = folio(b);
        assertMoney(f.getBalanceDue(), "0");
        assertMoney(f.getCreditBalance(), "1000");
        assertThat(revenueGross(ledger(b))).isEqualByComparingTo("2000"); // not the old 3 000
    }

    // --- Reprice ------------------------------------------------------------------------------

    @Test
    void repricingAPaidBooking_reopensIt() {
        RoomEntity standard = room("1000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        markPaid(b);
        standard.setBasePrice(new BigDecimal("1500.00"));
        roomRepository.saveAndFlush(standard);
        String segmentId = segmentRepository.findByBookingIdOrderByCheckInAsc(b.getId()).get(0).getId();

        RepriceQuote quote = bookingService.quoteReprice(b.getId(), new RepriceInput(segmentId));
        assertThat(quote.getReopensPaid()).isTrue();
        assertThat(quote.getPaidWarning()).isEqualTo("Room paid will be reopened: the guest will owe ฿1,500.");

        bookingService.reprice(b.getId(), new RepriceInput(segmentId));

        assertThat(status(b)).isEqualTo(BookingStatus.CONFIRMED);
        assertMoney(folio(b).getBalanceDue(), "1500");
        assertMoney(folio(b).getSettledOutside(), "0");
    }

    // --- Legacy: PAID by hand, the room collected outside the system ---------------------------

    @Test
    void aLegacyPaidRow_keepsItsOutsideSettlement_asAnOtherPayment_andOwesOnlyTheGrowth() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3); // 3 000
        // Hand-set PAID from before "PAID never writes money off": no folio payment, nothing posted.
        jdbc.update("UPDATE \"Booking\" SET status = 'PAID' WHERE id = ?", b.getId());
        assertMoney(folio(b).getSettledOutside(), "3000");
        assertMoney(folio(b).getBalanceDue(), "0");

        bookingService.relocate(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(status(b)).isEqualTo(BookingStatus.CONFIRMED);
        BookingFolio f = folio(b);
        assertMoney(f.getPaidTotal(), "3000");          // the outside settlement is now a real payment ...
        assertMoney(f.getSettledOutside(), "0");
        assertMoney(f.getBalanceDue(), "6000");         // ... so the room is not charged twice, only the growth is owed
        assertThat(bookingService.listFolioPayments(b.getId())).singleElement()
                .satisfies(p -> assertThat(p.getMethod()).isEqualTo(FolioPaymentMethod.OTHER));

        bookingService.recordFolioPayment(b.getId(), new FolioPaymentInput(FolioPaymentMethod.CASH, "6000.00"));
        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
        assertThat(ledgerService.trialBalance(TODAY.toString()).getBalanced()).isTrue();
        assertThat(revenueGross(ledger(b))).isEqualByComparingTo("9000");
    }

    // --- Not a staff caller -------------------------------------------------------------------

    @Test
    void aWriteWithNoStaffMember_leavesThePaidStatusAlone() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        markPaid(b);
        SecurityContextHolder.clearContext(); // e.g. the SiteMinder import: no person to record a payment under

        bookingService.relocate(b.getId(), move(TODAY.plusDays(5), suite, unit(suite, 0), RelocationRateMode.NEW_RATE), true, true);

        assertThat(status(b)).isEqualTo(BookingStatus.PAID);
    }

    // --- A checked-in guest needs a room (the cashier's relocation form) ----------------------

    @Test
    void movingACheckedInGuestToNoRoom_isRefused_onTheRoomUnitField() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY, 3);
        occupancyService.checkIn(b.getId());
        RelocationInput noUnit = new RelocationInput(TODAY.toString(), suite.getId()).roomUnitId((String) null);
        noUnit.setRateMode(RelocationRateMode.NEW_RATE);

        assertThatThrownBy(() -> bookingService.relocate(b.getId(), noUnit, false, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("A checked-in guest needs a room");
        assertThat(segmentRepository.findByBookingIdOrderByCheckInAsc(b.getId())).hasSize(1);
    }

    @Test
    void movingAGuestWhoHasNotArrived_toNoRoom_isStillAllowed() {
        RoomEntity standard = room("1000.00");
        RoomEntity suite = room("3000.00");
        Booking b = booking(standard, unit(standard, 0), TODAY.plusDays(5), 3);
        RelocationInput noUnit = new RelocationInput(TODAY.plusDays(5).toString(), suite.getId()).roomUnitId((String) null);
        noUnit.setRateMode(RelocationRateMode.NEW_RATE);

        Booking moved = bookingService.relocate(b.getId(), noUnit, false, false);

        assertThat(moved.getRoomId()).isEqualTo(suite.getId());
    }

    private SettlementStatus folioSettlement(Booking b) {
        return bookingService.getById(b.getId()).getSettlement();
    }
}
