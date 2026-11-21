package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationQuote;
import com.sunsetbeach.model.RelocationRateMode;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.support.FixedHotelClockConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.context.annotation.Import;

/**
 * Relocation from the first night of a segment (a guest arriving today) and the choice of rate
 * (keep the agreed prices, or the new room type's). "Today" is pinned to 2034-03-10. Deliberately
 * not {@code @Transactional}: the race test needs two real transactions, so every test cleans up
 * its own rooms.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
class BookingRelocationRateModeTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private BookingSegmentNightlyRateRepository nightlyRateRepository;

    private final List<String> roomIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        List<String> bookingIds = segmentRepository.findAll().stream()
                .filter(s -> roomIds.contains(s.getRoomId()))
                .map(BookingSegmentEntity::getBookingId)
                .distinct()
                .toList();
        if (!bookingIds.isEmpty()) {
            bookingRepository.deleteAllById(bookingIds);
        }
        for (String roomId : roomIds) {
            roomUnitRepository.deleteAll(roomUnitRepository.findByRoomId(roomId));
            roomRepository.deleteById(roomId);
        }
        roomIds.clear();
    }

    private RoomEntity room(String basePrice, int units) {
        RoomEntity r = new RoomEntity();
        r.setName("RateMode Room " + UUID.randomUUID());
        r.setDescription("Used only by BookingRelocationRateModeTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal(basePrice));
        RoomEntity saved = roomRepository.saveAndFlush(r);
        roomIds.add(saved.getId());
        for (int i = 0; i < units; i++) {
            RoomUnitEntity u = new RoomUnitEntity();
            u.setRoomId(saved.getId());
            u.setLabel("RM-" + UUID.randomUUID().toString().substring(0, 8));
            u.setActive(true);
            u.setHousekeepingStatus(HousekeepingStatus.CLEAN);
            roomUnitRepository.saveAndFlush(u);
        }
        return saved;
    }

    private RoomUnitEntity unit(RoomEntity room, int index) {
        return roomUnitRepository.findByRoomId(room.getId()).stream()
                .sorted(Comparator.comparing(RoomUnitEntity::getLabel))
                .toList()
                .get(index);
    }

    private Booking booking(RoomEntity room, RoomUnitEntity unit, LocalDate checkIn, int nights, String agreedTotal) {
        StaffBookingCreateInput input = new StaffBookingCreateInput(
                room.getId(), "Guest " + UUID.randomUUID(), checkIn.toString(), checkIn.plusDays(nights).toString(), BookingChannel.WALK_IN, 1);
        if (agreedTotal != null) {
            input.agreedTotal(agreedTotal);
        }
        Booking b = bookingService.createStaffBooking(input);
        bookingService.assignRoomUnit(b.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        return b;
    }

    private List<BookingSegmentEntity> segments(String bookingId) {
        return segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
    }

    private List<BigDecimal> ratesOf(BookingSegmentEntity segment) {
        return nightlyRateRepository.findBySegmentIdOrderByDateAsc(segment.getId()).stream()
                .filter(r -> !r.getDate().isBefore(segment.getCheckIn()) && r.getDate().isBefore(segment.getCheckOut()))
                .map(BookingSegmentNightlyRateEntity::getPrice)
                .toList();
    }

    private RelocationInput move(LocalDate from, RoomEntity room, RoomUnitEntity unit, RelocationRateMode mode) {
        RelocationInput in = new RelocationInput(from.toString(), room.getId()).roomUnitId(unit.getId());
        in.setRateMode(mode);
        return in;
    }

    private HousekeepingStatus statusOf(RoomUnitEntity u) {
        return roomUnitRepository.findById(u.getId()).orElseThrow().getHousekeepingStatus();
    }

    private static void assertMoney(String actual, String expected) {
        assertThat(new BigDecimal(actual)).isEqualByComparingTo(expected);
    }

    // --- First night: the whole segment moves ---------------------------------------------------

    @Test
    void firstNightMove_toAnotherType_newRate_repricesEveryNight_andDoesNotSplit() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking b = booking(standard, unit(standard, 0), TODAY, 3, null);

        Booking moved = bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(segments(b.getId())).hasSize(1);
        assertThat(segments(b.getId()).get(0).getRoomId()).isEqualTo(suite.getId());
        assertThat(moved.getRoomId()).isEqualTo(suite.getId());
        assertMoney(moved.getTotalPrice(), "9000.00");
        assertThat(ratesOf(segments(b.getId()).get(0))).allSatisfy(p -> assertThat(p).isEqualByComparingTo("3000.00")).hasSize(3);
    }

    @Test
    void firstNightMove_toAnotherType_keepRate_keepsEveryNightsAgreedPrice() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking b = booking(standard, unit(standard, 0), TODAY, 3, "2500.00"); // a hand-typed package price

        List<BigDecimal> before = ratesOf(segments(b.getId()).get(0));
        Booking moved = bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.KEEP_RATE), true, false);

        assertMoney(moved.getTotalPrice(), "2500.00");
        assertThat(segments(b.getId())).hasSize(1);
        assertThat(segments(b.getId()).get(0).getRoomId()).isEqualTo(suite.getId());
        assertThat(ratesOf(segments(b.getId()).get(0))).containsExactlyElementsOf(before);
    }

    @Test
    void defaultMode_acrossTypes_isNewRate() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking b = booking(standard, unit(standard, 0), TODAY, 2, null);

        Booking moved = bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), null), false, false);

        assertMoney(moved.getTotalPrice(), "6000.00");
    }

    @Test
    void guestArrivingTodayAndCheckedIn_oldRoomIsDirty_afterAFirstNightMove() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        RoomUnitEntity old = unit(standard, 0);
        Booking b = booking(standard, old, TODAY, 2, null);
        occupancyService.checkIn(b.getId());
        assertThat(statusOf(old)).isEqualTo(HousekeepingStatus.CLEAN);

        bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(statusOf(old)).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void guestNotYetCheckedIn_oldRoomIsLeftAlone_afterAFirstNightMove() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        RoomUnitEntity old = unit(standard, 0);
        Booking b = booking(standard, old, TODAY, 2, null);

        bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        assertThat(statusOf(old)).isEqualTo(HousekeepingStatus.CLEAN);
    }

    // --- Mid-stay from today: the past stays where it was --------------------------------------

    @Test
    void checkedInGuest_movedFromToday_pastNightsStayOnTheOldRoomAtTheirPrice() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        RoomUnitEntity old = unit(standard, 0);
        Booking b = booking(standard, old, TODAY.minusDays(2), 5, null); // 03-08 .. 03-13
        occupancyService.checkIn(b.getId());

        Booking moved = bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);

        List<BookingSegmentEntity> segs = segments(b.getId());
        assertThat(segs).hasSize(2);
        assertThat(segs.get(0).getRoomUnitId()).isEqualTo(old.getId());
        assertThat(segs.get(0).getCheckOut()).isEqualTo(TODAY);
        assertThat(ratesOf(segs.get(0))).hasSize(2).allSatisfy(p -> assertThat(p).isEqualByComparingTo("1000.00"));
        assertThat(ratesOf(segs.get(1))).hasSize(3).allSatisfy(p -> assertThat(p).isEqualByComparingTo("3000.00"));
        assertMoney(moved.getTotalPrice(), "11000.00");
        assertThat(statusOf(old)).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void keepRate_onASplit_keepsAManualPrice_andUndoRestoresTheOriginalNights() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking b = booking(standard, unit(standard, 0), TODAY, 3, "2500.00"); // 833.33 / 833.33 / 833.34
        List<BigDecimal> original = ratesOf(segments(b.getId()).get(0));

        Booking moved = bookingService.relocate(b.getId(), move(TODAY.plusDays(1), suite, unit(suite, 0), RelocationRateMode.KEEP_RATE), true, false);

        assertMoney(moved.getTotalPrice(), "2500.00");
        List<BookingSegmentEntity> segs = segments(b.getId());
        assertThat(ratesOf(segs.get(1))).containsExactlyElementsOf(original.subList(1, 3));

        Booking undone = bookingService.undoRelocation(b.getId(), new RelocationUndoInput(TODAY.plusDays(1).toString()));
        assertMoney(undone.getTotalPrice(), "2500.00");
        assertThat(ratesOf(segments(b.getId()).get(0))).containsExactlyElementsOf(original);
    }

    // --- Same category ------------------------------------------------------------------------

    @Test
    void sameType_neverChangesThePrice_evenWhenTheRateWentUp() {
        RoomEntity standard = room("1000.00", 2);
        Booking b = booking(standard, unit(standard, 0), TODAY, 3, null);
        RoomEntity raised = roomRepository.findById(standard.getId()).orElseThrow();
        raised.setBasePrice(new BigDecimal("2000.00"));
        roomRepository.saveAndFlush(raised);

        Booking firstNight = bookingService.relocate(b.getId(), move(TODAY, standard, unit(standard, 1), null), false, false);
        assertMoney(firstNight.getTotalPrice(), "3000.00");

        Booking split = bookingService.relocate(b.getId(), move(TODAY.plusDays(1), standard, unit(standard, 0), null), false, false);
        assertMoney(split.getTotalPrice(), "3000.00");
    }

    @Test
    void sameType_newRate_isRefused_andSoIsMovingToTheRoomTheGuestIsIn() {
        RoomEntity standard = room("1000.00", 2);
        Booking b = booking(standard, unit(standard, 0), TODAY, 3, null);

        assertThatThrownBy(() -> bookingService.relocate(b.getId(), move(TODAY, standard, unit(standard, 1), RelocationRateMode.NEW_RATE), true, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reprice");
        assertThatThrownBy(() -> bookingService.relocate(b.getId(), move(TODAY, standard, unit(standard, 0), null), true, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already in that room");
        assertThat(segments(b.getId())).hasSize(1);
    }

    // --- Roles ----------------------------------------------------------------------------------

    @Test
    void cashier_cannotKeepTheOriginalRateAcrossTypes_butCanApplyTheNewOne() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking b = booking(standard, unit(standard, 0), TODAY, 2, null);

        assertThatThrownBy(() -> bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.KEEP_RATE), false, false))
                .isInstanceOf(ForbiddenException.class);
        assertThat(segments(b.getId()).get(0).getRoomId()).as("nothing changed").isEqualTo(standard.getId());

        Booking moved = bookingService.relocate(b.getId(), move(TODAY, suite, unit(suite, 0), RelocationRateMode.NEW_RATE), false, false);
        assertMoney(moved.getTotalPrice(), "6000.00");
    }

    @Test
    void cashier_cannotMoveFromADateInThePast_amanagerCan() {
        RoomEntity standard = room("1000.00", 2);
        Booking b = booking(standard, unit(standard, 0), TODAY.minusDays(2), 5, null);

        assertThatThrownBy(() -> bookingService.relocate(b.getId(), move(TODAY.minusDays(1), standard, unit(standard, 1), null), false, false))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> bookingService.quoteRelocation(b.getId(), move(TODAY.minusDays(1), standard, unit(standard, 1), null), false, false))
                .isInstanceOf(ForbiddenException.class);

        bookingService.relocate(b.getId(), move(TODAY.minusDays(1), standard, unit(standard, 1), null), false, true);
        assertThat(segments(b.getId())).hasSize(2);
    }

    // --- Quote = result -------------------------------------------------------------------------

    @Test
    void quote_showsBothRatesWithTheirDifferences_andMatchesWhatApplyingCharges() {
        RoomEntity standard = room("1000.00", 1);
        RoomEntity suite = room("3000.00", 1);
        Booking newRate = booking(standard, unit(standard, 0), TODAY, 3, "2400.00");
        RelocationInput input = move(TODAY.plusDays(1), suite, unit(suite, 0), null);

        RelocationQuote cashierView = bookingService.quoteRelocation(newRate.getId(), input, false, false);
        assertMoney(cashierView.getCurrentTotalPrice(), "2400.00");
        assertThat(cashierView.getSameRoomType()).isFalse();
        assertThat(cashierView.getMayKeepRate()).isFalse();
        assertThat(cashierView.getRateMode()).isEqualTo(RelocationRateMode.NEW_RATE);
        assertMoney(cashierView.getKeepRateTotalPrice(), "2400.00");
        assertMoney(cashierView.getKeepRatePriceDifference(), "0.00");
        // 1 night kept at 800 + 2 nights at 3000
        assertMoney(cashierView.getNewRateTotalPrice().get(), "6800.00");
        assertMoney(cashierView.getNewRatePriceDifference().get(), "4400.00");
        assertThat(cashierView.getAvailable()).isTrue();

        Booking applied = bookingService.relocate(newRate.getId(), input, false, false);
        assertThat(applied.getTotalPrice()).isEqualTo(cashierView.getTotalPrice());

        // KEEP_RATE, as a manager, on a second booking
        Booking keep = booking(standard, unit(standard, 0), TODAY.plusDays(10), 3, "2400.00");
        RelocationInput keepInput = move(TODAY.plusDays(11), suite, unit(suite, 0), RelocationRateMode.KEEP_RATE);
        RelocationQuote managerView = bookingService.quoteRelocation(keep.getId(), keepInput, true, false);
        assertThat(managerView.getMayKeepRate()).isTrue();
        Booking keptApplied = bookingService.relocate(keep.getId(), keepInput, true, false);
        assertThat(keptApplied.getTotalPrice()).isEqualTo(managerView.getTotalPrice());
        assertMoney(keptApplied.getTotalPrice(), "2400.00");
    }

    @Test
    void quote_sameType_reportsNoNewRate_andRefusesNewRateAsUnavailable() {
        RoomEntity standard = room("1000.00", 2);
        Booking b = booking(standard, unit(standard, 0), TODAY, 2, null);

        RelocationQuote quote = bookingService.quoteRelocation(b.getId(), move(TODAY, standard, unit(standard, 1), null), false, false);
        assertThat(quote.getSameRoomType()).isTrue();
        assertThat(quote.getWholeSegment()).isTrue();
        assertThat(quote.getNewRateTotalPrice().isPresent()).isFalse();
        assertMoney(quote.getKeepRatePriceDifference(), "0.00");
        assertThat(quote.getAvailable()).isTrue();

        RelocationQuote refused = bookingService.quoteRelocation(b.getId(), move(TODAY, standard, unit(standard, 1), RelocationRateMode.NEW_RATE), false, false);
        assertThat(refused.getAvailable()).isFalse();
        assertThat(refused.getReason().get()).contains("reprice");
    }

    // --- Race -----------------------------------------------------------------------------------

    @Test
    void twoFirstNightMovesIntoTheSameOnlyUnit_exactlyOneWins() throws Exception {
        RoomEntity standard = room("1000.00", 2);
        RoomEntity suite = room("3000.00", 1);
        Booking one = booking(standard, unit(standard, 0), TODAY, 2, null);
        Booking two = booking(standard, unit(standard, 1), TODAY, 2, null);
        RoomUnitEntity target = unit(suite, 0);

        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Callable<Object>> jobs = List.of(one, two).stream().<Callable<Object>>map(b -> () -> {
            barrier.await();
            try {
                return bookingService.relocate(b.getId(), move(TODAY, suite, target, RelocationRateMode.NEW_RATE), false, false);
            } catch (Exception e) {
                return e;
            }
        }).toList();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Object> results = new ArrayList<>();
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> job : jobs) {
                futures.add(pool.submit(job));
            }
            for (Future<Object> f : futures) {
                results.add(f.get());
            }
        } finally {
            pool.shutdown();
        }

        assertThat(results.stream().filter(r -> r instanceof Booking).count()).isEqualTo(1);
        assertThat(results.stream().filter(r -> r instanceof ConflictException).count()).isEqualTo(1);
        long inSuite = List.of(one, two).stream().filter(b -> segments(b.getId()).get(0).getRoomId().equals(suite.getId())).count();
        assertThat(inSuite).isEqualTo(1);
    }
}
