package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RepriceInput;
import com.sunsetbeach.model.RepriceQuote;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the fix for a real production bug: a segment's price used to be recomputed from
 * *current* RatePlan/Room.basePrice every time its dates changed, silently repricing nights the
 * guest had already been quoted whenever a rate changed after booking and staff later extended,
 * shrank, or relocated that stay (a 6-night ฿18,000 booking, extended by one night, previewed at
 * ฿41,000 - all 7 nights repriced, not just the new one). Every test here deliberately changes
 * the room's rate *after* the booking already exists, then exercises a write, and checks that
 * only the nights actually being added/removed/explicitly-repriced ever change price - see
 * BookingWriter's class javadoc ("Nightly price snapshots") for the full design.
 *
 * <p>DB-backed against the ephemeral Testcontainers Postgres, {@code @Transactional} (auto
 * rollback) - no concurrency under test here, unlike BookingRelocationTests's race test.
 */
@SpringBootTest
@Transactional
class BookingRepricingTests extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository segmentRepository;

    @Autowired
    private BookingSegmentNightlyRateRepository nightlyRateRepository;

    private RoomEntity createRoom(BigDecimal basePrice) {
        RoomEntity room = new RoomEntity();
        room.setName("Repricing Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingRepricingTests");
        room.setCapacity(2);
        room.setBasePrice(basePrice);
        RoomEntity saved = roomRepository.saveAndFlush(room);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(saved.getId());
        unit.setLabel("Repricing Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        return saved;
    }

    private Booking createBooking(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingService.createBooking(
                new BookingCreateInput(roomId, "Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString()));
    }

    private List<BookingSegmentEntity> segmentsOf(String bookingId) {
        return segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
    }

    private Map<LocalDate, BigDecimal> nightlyRatesOf(String segmentId) {
        return nightlyRateRepository.findBySegmentIdOrderByDateAsc(segmentId).stream()
                .collect(Collectors.toMap(BookingSegmentNightlyRateEntity::getDate, BookingSegmentNightlyRateEntity::getPrice));
    }

    private void bumpBasePrice(RoomEntity room, BigDecimal newBasePrice) {
        room.setBasePrice(newBasePrice);
        roomRepository.saveAndFlush(room);
    }

    // --- Extending a stay must not touch already-agreed nights ----------------------------------

    @Test
    void extendingAStay_afterARateHike_onlyPricesTheNewNightAtTheNewRate() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(300);
        LocalDate checkOut = checkIn.plusDays(6); // 6 nights @ 1000 = 6000, matching the reported bug's shape
        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        assertThat(new BigDecimal(booking.getTotalPrice())).isEqualByComparingTo("6000.00");

        // The rate changes AFTER the booking exists - this is the whole scenario.
        bumpBasePrice(room, new BigDecimal("5000.00"));

        LocalDate extendedCheckOut = checkOut.plusDays(1);
        BookingScheduleInput input = new BookingScheduleInput(checkIn.toString(), extendedCheckOut.toString()).roomUnitId(null);

        BookingScheduleQuote quote = bookingService.quoteSchedule(booking.getId(), input);
        assertThat(quote.getAvailable()).isTrue();
        // 6000 (unchanged) + 5000 (one new night at the new rate) = 11000 - NOT 7 x 5000 = 35000.
        assertThat(new BigDecimal(quote.getTotalPrice())).isEqualByComparingTo("11000.00");

        Booking updated = bookingService.updateSchedule(booking.getId(), input);
        assertThat(new BigDecimal(updated.getTotalPrice())).isEqualByComparingTo("11000.00");

        BookingSegmentEntity segment = segmentsOf(booking.getId()).get(0);
        assertThat(segment.getTotalPrice()).isEqualByComparingTo("11000.00");
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(segment.getId());
        assertThat(rates).hasSize(7);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            assertThat(rates.get(night)).as("original night %s must keep its original price", night).isEqualByComparingTo("1000.00");
        }
        assertThat(rates.get(checkOut)).as("the new night must be priced at the new rate").isEqualByComparingTo("5000.00");
    }

    @Test
    void extendingEarlierArrival_afterARateHike_onlyPricesTheNewNightAtTheNewRate() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(305);
        LocalDate checkOut = checkIn.plusDays(3);
        Booking booking = createBooking(room.getId(), checkIn, checkOut);

        bumpBasePrice(room, new BigDecimal("5000.00"));

        LocalDate earlierCheckIn = checkIn.minusDays(1);
        Booking updated = bookingService.updateSchedule(
                booking.getId(), new BookingScheduleInput(earlierCheckIn.toString(), checkOut.toString()).roomUnitId(null));

        // 3000 (unchanged) + 5000 (one new earlier night) = 8000.
        assertThat(new BigDecimal(updated.getTotalPrice())).isEqualByComparingTo("8000.00");
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(segmentsOf(booking.getId()).get(0).getId());
        assertThat(rates.get(earlierCheckIn)).isEqualByComparingTo("5000.00");
        assertThat(rates.get(checkIn)).isEqualByComparingTo("1000.00");
    }

    // --- Shrinking a stay must only drop the removed nights, never reprice what remains ---------

    @Test
    void shrinkingAStay_afterARateHike_onlyDropsTheRemovedNights_neverReprices() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(310);
        LocalDate checkOut = checkIn.plusDays(6); // 6 nights @ 1000 = 6000
        Booking booking = createBooking(room.getId(), checkIn, checkOut);

        bumpBasePrice(room, new BigDecimal("5000.00"));

        LocalDate shorterCheckOut = checkOut.minusDays(2); // drop the last 2 nights
        Booking updated = bookingService.updateSchedule(
                booking.getId(), new BookingScheduleInput(checkIn.toString(), shorterCheckOut.toString()).roomUnitId(null));

        // 6000 - 2000 (the two dropped nights, at their ORIGINAL price) = 4000 - not 4 x 5000 = 20000.
        assertThat(new BigDecimal(updated.getTotalPrice())).isEqualByComparingTo("4000.00");
        BookingSegmentEntity segment = segmentsOf(booking.getId()).get(0);
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(segment.getId());
        assertThat(rates).hasSize(4);
        rates.values().forEach(price -> assertThat(price).isEqualByComparingTo("1000.00"));
    }

    // --- Relocation: the room the guest is leaving must not be repriced either -------------------

    @Test
    void relocating_afterARateHikeInTheOldRoom_leavesTheRemainingOldNightsAtTheirOriginalPrice() {
        RoomEntity oldRoom = createRoom(new BigDecimal("1000.00"));
        RoomEntity newRoom = createRoom(new BigDecimal("2000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(315);
        LocalDate checkOut = checkIn.plusDays(4); // 4 nights @ 1000 = 4000
        LocalDate splitDate = checkIn.plusDays(2); // 2 nights stay in oldRoom, 2 nights move to newRoom
        Booking booking = createBooking(oldRoom.getId(), checkIn, checkOut);

        // The old room's rate goes up AFTER booking, before the guest is relocated out of it -
        // this is the second, deeper instance of the bug: relocate used to recompute the
        // shrinking old segment's remaining nights from the CURRENT rate too.
        bumpBasePrice(oldRoom, new BigDecimal("9000.00"));
        RoomUnitEntity newUnit = roomUnitRepository.findByRoomId(newRoom.getId()).get(0);

        Booking relocated = bookingService.relocate(
                booking.getId(), new RelocationInput(splitDate.toString(), newRoom.getId()).roomUnitId(newUnit.getId()));

        // Old segment: 2 nights at the ORIGINAL 1000 (never repriced) = 2000.
        // New segment: 2 nights at newRoom's current rate 2000 = 4000.
        // Total: 6000 - not 2x9000 + 2x2000 = 22000.
        assertThat(new BigDecimal(relocated.getTotalPrice())).isEqualByComparingTo("6000.00");

        List<BookingSegmentEntity> segments = segmentsOf(booking.getId());
        BookingSegmentEntity oldSegment = segments.stream().filter(s -> s.getRoomId().equals(oldRoom.getId())).findFirst().orElseThrow();
        BookingSegmentEntity newSegment = segments.stream().filter(s -> s.getRoomId().equals(newRoom.getId())).findFirst().orElseThrow();
        assertThat(oldSegment.getTotalPrice()).isEqualByComparingTo("2000.00");
        assertThat(newSegment.getTotalPrice()).isEqualByComparingTo("4000.00");
        nightlyRatesOf(oldSegment.getId()).values().forEach(price -> assertThat(price).isEqualByComparingTo("1000.00"));
        nightlyRatesOf(newSegment.getId()).values().forEach(price -> assertThat(price).isEqualByComparingTo("2000.00"));
    }

    @Test
    void quoteRelocation_afterARateHikeInTheOldRoom_previewsTheSameUnrepricedTotal() {
        RoomEntity oldRoom = createRoom(new BigDecimal("1000.00"));
        RoomEntity newRoom = createRoom(new BigDecimal("2000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(320);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        Booking booking = createBooking(oldRoom.getId(), checkIn, checkOut);
        bumpBasePrice(oldRoom, new BigDecimal("9000.00"));
        RoomUnitEntity newUnit = roomUnitRepository.findByRoomId(newRoom.getId()).get(0);

        BookingScheduleQuote quote = bookingService.quoteRelocation(
                booking.getId(), new RelocationInput(splitDate.toString(), newRoom.getId()).roomUnitId(newUnit.getId()));

        assertThat(quote.getAvailable()).isTrue();
        assertThat(new BigDecimal(quote.getTotalPrice())).isEqualByComparingTo("6000.00");
        // A preview must not have written anything - the old segment is still untouched.
        assertThat(segmentsOf(booking.getId())).hasSize(1);
    }

    // --- Undo-relocation is a real undo: it restores each night's ORIGINAL price, regardless of
    // what either room's rate has done since - not a fresh reprice at the reverted room's current
    // rate. See BookingWriter#undoRelocation's javadoc for why an earlier version of this test
    // asserted the opposite: it was written before nightly snapshots existed, when every
    // schedule-touching write repriced wholesale, and it happened to still pass under a "reprice
    // on revert" rule too only because it never changed a rate between relocate and undo - so it
    // never actually distinguished "restore" from "reprice," despite looking like it confirmed
    // one. A test that can't tell two designs apart isn't evidence for either. This version
    // deliberately hikes both rooms' rates between the relocation and the undo, which the old
    // version never did - that's the one thing that actually distinguishes "restore the original
    // price" from "reprice at today's rate," and it's the case a real guest complaint looks like:
    // staff relocate, reconsider, and undo, with a rate change landing in between purely by
    // coincidence of timing, not because anyone renegotiated anything. --------------------------

    @Test
    void undoRelocation_restoresEachNightsOriginalPrice_ignoringRateChangesSinceTheRelocation() {
        RoomEntity oldRoom = createRoom(new BigDecimal("1000.00"));
        RoomEntity newRoom = createRoom(new BigDecimal("2000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(325);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        Booking booking = createBooking(oldRoom.getId(), checkIn, checkOut);
        RoomUnitEntity newUnit = roomUnitRepository.findByRoomId(newRoom.getId()).get(0);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), newRoom.getId()).roomUnitId(newUnit.getId()));
        // At this point: old segment 2 nights @ 1000 = 2000, new segment 2 nights @ 2000 = 4000, total 6000.

        // Both rooms' rates move again before the relocation is undone - a guest who asked for
        // none of this must not see either hike on their bill just because staff reconsidered.
        bumpBasePrice(oldRoom, new BigDecimal("7777.00"));
        bumpBasePrice(newRoom, new BigDecimal("8888.00"));

        Booking undone = bookingService.undoRelocation(booking.getId(), new RelocationUndoInput(splitDate.toString()));

        // Exactly what the booking cost before it was ever relocated - as if the move (and both
        // subsequent rate hikes) never happened. NOT 2000 + 2x7777 = 17554, which is what a
        // reprice-on-revert rule would have produced.
        assertThat(new BigDecimal(undone.getTotalPrice())).isEqualByComparingTo("4000.00");
        BookingSegmentEntity merged = segmentsOf(booking.getId()).get(0);
        assertThat(merged.getTotalPrice()).isEqualByComparingTo("4000.00");
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(merged.getId());
        assertThat(rates).hasSize(4);
        rates.values().forEach(price -> assertThat(price).isEqualByComparingTo("1000.00"));
    }

    @Test
    void undoRelocation_fallsBackToTodaysRateOnlyForNightsWhoseOriginalRowIsGenuinelyMissing() {
        // Simulates a relocation made before this restore-on-undo design existed (or any other
        // gap that dropped the original row) - the fallback exists so undo still completes with
        // the best available answer instead of failing outright, but it must not spread beyond
        // the specific nights actually missing their history.
        RoomEntity oldRoom = createRoom(new BigDecimal("1000.00"));
        RoomEntity newRoom = createRoom(new BigDecimal("2000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(340);
        LocalDate checkOut = checkIn.plusDays(4);
        LocalDate splitDate = checkIn.plusDays(2);
        Booking booking = createBooking(oldRoom.getId(), checkIn, checkOut);
        RoomUnitEntity newUnit = roomUnitRepository.findByRoomId(newRoom.getId()).get(0);
        bookingService.relocate(booking.getId(), new RelocationInput(splitDate.toString(), newRoom.getId()).roomUnitId(newUnit.getId()));

        String oldSegmentId = segmentsOf(booking.getId()).stream()
                .filter(s -> s.getRoomId().equals(oldRoom.getId())).findFirst().orElseThrow().getId();
        // Deliberately destroy the preserved history for just one of the two reverting nights,
        // as if this relocation predated the design that keeps it.
        nightlyRateRepository.deleteBySegmentIdAndDateIn(oldSegmentId, List.of(splitDate));

        bumpBasePrice(oldRoom, new BigDecimal("7777.00"));

        Booking undone = bookingService.undoRelocation(booking.getId(), new RelocationUndoInput(splitDate.toString()));

        // splitDate lost its history -> falls back to oldRoom's current (hiked) rate, 7777.
        // splitDate+1 still has its original oldRoom-at-1000 row -> restored untouched, 1000.
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(segmentsOf(booking.getId()).get(0).getId());
        assertThat(rates.get(checkIn)).isEqualByComparingTo("1000.00");
        assertThat(rates.get(checkIn.plusDays(1))).isEqualByComparingTo("1000.00");
        assertThat(rates.get(splitDate)).isEqualByComparingTo("7777.00"); // fell back - history was destroyed
        assertThat(rates.get(splitDate.plusDays(1))).isEqualByComparingTo("1000.00"); // restored - history survived
        // 2000 (before's own untouched nights) + 7777 (fallback) + 1000 (restored) = 10777.
        assertThat(new BigDecimal(undone.getTotalPrice())).isEqualByComparingTo("10777.00");
    }

    // --- Explicit reprice: the one deliberate way to move an agreed price forward -----------------

    @Test
    void reprice_movesTheWholeSegmentToTheCurrentRate_whenItIsEntirelyInTheFuture() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate checkIn = LocalDate.now().plusDays(330);
        LocalDate checkOut = checkIn.plusDays(3); // 3000
        Booking booking = createBooking(room.getId(), checkIn, checkOut);
        bumpBasePrice(room, new BigDecimal("4000.00"));
        String segmentId = segmentsOf(booking.getId()).get(0).getId();

        RepriceQuote quote = bookingService.quoteReprice(booking.getId(), new RepriceInput(segmentId));
        assertThat(new BigDecimal(quote.getOldTotalPrice())).isEqualByComparingTo("3000.00");
        assertThat(new BigDecimal(quote.getNewTotalPrice())).isEqualByComparingTo("12000.00");
        assertThat(quote.getNightsRepriced()).isEqualTo(3);
        // A preview must not have written anything.
        assertThat(segmentsOf(booking.getId()).get(0).getTotalPrice()).isEqualByComparingTo("3000.00");

        Booking repriced = bookingService.reprice(booking.getId(), new RepriceInput(segmentId));
        assertThat(new BigDecimal(repriced.getTotalPrice())).isEqualByComparingTo("12000.00");
        nightlyRatesOf(segmentId).values().forEach(price -> assertThat(price).isEqualByComparingTo("4000.00"));
    }

    @Test
    void reprice_leavesAlreadyStayedNightsUntouched_onlyRepricesFromToday() {
        // Built directly via repositories (bypassing BookingService's own validation) since this
        // needs a segment straddling "today" - exactly the case a booking created through the
        // normal create flow never starts in, but a genuinely mid-stay booking always eventually
        // reaches.
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate today = LocalDate.now();
        LocalDate checkIn = today.minusDays(2);
        LocalDate checkOut = today.plusDays(3); // nights: -2, -1, 0(today), +1, +2 = 5 nights @ 1000 = 5000

        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(new BigDecimal("5000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(savedBooking.getId());
        segment.setRoomId(room.getId());
        segment.setCheckIn(checkIn);
        segment.setCheckOut(checkOut);
        segment.setTotalPrice(new BigDecimal("5000.00"));
        BookingSegmentEntity savedSegment = segmentRepository.saveAndFlush(segment);

        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            BookingSegmentNightlyRateEntity rate = new BookingSegmentNightlyRateEntity();
            rate.setSegmentId(savedSegment.getId());
            rate.setDate(night);
            rate.setPrice(new BigDecimal("1000.00"));
            nightlyRateRepository.saveAndFlush(rate);
        }

        bumpBasePrice(room, new BigDecimal("3000.00"));

        Booking repriced = bookingService.reprice(savedBooking.getId(), new RepriceInput(savedSegment.getId()));

        // Nights -2 and -1 (already stayed) keep 1000 each = 2000. Nights 0, +1, +2 move to
        // 3000 each = 9000. Total 11000 - not 5 x 3000 = 15000, and not left at 5000 either.
        assertThat(new BigDecimal(repriced.getTotalPrice())).isEqualByComparingTo("11000.00");
        Map<LocalDate, BigDecimal> rates = nightlyRatesOf(savedSegment.getId());
        assertThat(rates.get(checkIn)).isEqualByComparingTo("1000.00");
        assertThat(rates.get(checkIn.plusDays(1))).isEqualByComparingTo("1000.00");
        assertThat(rates.get(today)).isEqualByComparingTo("3000.00");
        assertThat(rates.get(today.plusDays(1))).isEqualByComparingTo("3000.00");
        assertThat(rates.get(today.plusDays(2))).isEqualByComparingTo("3000.00");
    }

    @Test
    void reprice_onASegmentEntirelyInThePast_isANoOp() {
        RoomEntity room = createRoom(new BigDecimal("1000.00"));
        LocalDate today = LocalDate.now();
        LocalDate checkIn = today.minusDays(5);
        LocalDate checkOut = today.minusDays(1); // entirely in the past

        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(new BigDecimal("4000.00"));
        booking.setStatus(BookingStatus.PAID);
        BookingEntity savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(savedBooking.getId());
        segment.setRoomId(room.getId());
        segment.setCheckIn(checkIn);
        segment.setCheckOut(checkOut);
        segment.setTotalPrice(new BigDecimal("4000.00"));
        BookingSegmentEntity savedSegment = segmentRepository.saveAndFlush(segment);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            BookingSegmentNightlyRateEntity rate = new BookingSegmentNightlyRateEntity();
            rate.setSegmentId(savedSegment.getId());
            rate.setDate(night);
            rate.setPrice(new BigDecimal("1000.00"));
            nightlyRateRepository.saveAndFlush(rate);
        }

        bumpBasePrice(room, new BigDecimal("9000.00"));

        RepriceQuote quote = bookingService.quoteReprice(savedBooking.getId(), new RepriceInput(savedSegment.getId()));
        assertThat(quote.getNightsRepriced()).isEqualTo(0);
        assertThat(new BigDecimal(quote.getOldTotalPrice())).isEqualByComparingTo("4000.00");
        assertThat(new BigDecimal(quote.getNewTotalPrice())).isEqualByComparingTo("4000.00");

        Booking repriced = bookingService.reprice(savedBooking.getId(), new RepriceInput(savedSegment.getId()));
        assertThat(new BigDecimal(repriced.getTotalPrice())).isEqualByComparingTo("4000.00");
    }
}
