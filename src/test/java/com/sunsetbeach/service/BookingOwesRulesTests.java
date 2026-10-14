package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.NightAudit;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.service.BookingFolioCalculator.FolioBreakdown;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.sunsetbeach.support.FixedHotelClockConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * When an unpaid balance is a debt (owner's decisions after the second QA round): {@code
 * Booking.owes} is true only once the debt has fallen due - a guest in the house or gone, a
 * cancelled booking with charges, a stay whose dates are over - and never for a future booking,
 * however unpaid; and a no-show's room is not owed at all, so a no-show reaches "Unpaid balances"
 * only when something else was charged to it. "Today" is pinned to 2034-03-10, Bangkok.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class BookingOwesRulesTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private NightAuditService nightAuditService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;

    private static BookingEntity booking(BookingStatus status, OccupancyStatus occupancy, LocalDate checkIn, LocalDate checkOut) {
        BookingEntity b = new BookingEntity();
        b.setStatus(status);
        b.setOccupancyStatus(occupancy);
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        return b;
    }

    private static final BigDecimal OWED = new BigDecimal("500.00");

    @Test
    void aFutureUnpaidBooking_doesNotOwe_yet() {
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, TODAY.plusDays(3), TODAY.plusDays(5)), OWED, TODAY))
                .isFalse();
        // Arriving today but not here yet: still to be collected, not owed.
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.NEW, OccupancyStatus.EXPECTED, TODAY, TODAY.plusDays(2)), OWED, TODAY))
                .isFalse();
    }

    @Test
    void aGuestInTheHouse_aGuestGone_anOverdueDeparture_aCancelledBookingWithCharges_andAFinishedStay_owe() {
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_IN, TODAY, TODAY.plusDays(2)), OWED, TODAY))
                .isTrue();
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_IN, TODAY.minusDays(5), TODAY.minusDays(2)), OWED, TODAY))
                .as("overdue departure").isTrue();
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_OUT, TODAY.minusDays(3), TODAY), OWED, TODAY))
                .isTrue();
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CANCELLED, OccupancyStatus.EXPECTED, TODAY.plusDays(3), TODAY.plusDays(5)), OWED, TODAY))
                .as("cancelled with charges left").isTrue();
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.NEW, OccupancyStatus.EXPECTED, TODAY.minusDays(5), TODAY.minusDays(3)), OWED, TODAY))
                .as("dates over, never arrived").isTrue();
    }

    @Test
    void nothingOwed_isNeverADebt() {
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        booking(BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_OUT, TODAY.minusDays(3), TODAY), BigDecimal.ZERO, TODAY))
                .isFalse();
    }

    @Test
    void aNoShowsRoom_isNotOwed_butOtherChargesStillAre() {
        BigDecimal stay = new BigDecimal("3000.00");
        FolioBreakdown noShow = new FolioBreakdown(stay, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.NO_SHOW, false);
        assertThat(noShow.balanceDue()).isEqualByComparingTo("0.00");

        FolioBreakdown withCharges = new FolioBreakdown(stay, BigDecimal.ZERO, new BigDecimal("450.00"), 1, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.NO_SHOW, false);
        assertThat(withCharges.balanceDue()).isEqualByComparingTo("450.00");

        // The same stay, had the guest come, is owed in full.
        FolioBreakdown arrived = new FolioBreakdown(stay, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.CHECKED_OUT, false);
        assertThat(arrived.balanceDue()).isEqualByComparingTo("3000.00");
    }

    @Test
    void aNoShow_ofAnUnpaidPastStay_owesNothing_andIsNotInTheUnpaidBalancesOfTheNightAudit() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Owes Room " + UUID.randomUUID());
        newRoom.setDescription("Used only by BookingOwesRulesTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity room = roomRepository.saveAndFlush(newRoom);
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("Owes-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(true);
        roomUnitRepository.saveAndFlush(unit);
        Booking created = bookingService.createStaffBooking(new StaffBookingCreateInput(
                room.getId(), "No Show Guest", TODAY.minusDays(4).toString(), TODAY.minusDays(2).toString(), BookingChannel.WALK_IN, 1));
        String id = created.getId();

        // Dates passed, nobody arrived, not yet labelled: the room is not a debt - the night audit asks
        // the desk to mark it no-show or cancel it instead.
        Booking notArrived = bookingService.getById(id);
        assertThat(notArrived.getBalanceDue()).isEqualTo("0.00");
        assertThat(notArrived.getOwes()).isFalse();
        assertThat(unpaidIds()).doesNotContain(id);
        NightAudit audit = nightAuditService.get(TODAY.toString());
        assertThat(audit.getNotArrived().stream().map(b -> b.getId())).contains(id);
        assertThat(audit.getMissedArrivals().stream().map(b -> b.getId())).doesNotContain(id);

        BookingEntity entity = bookingRepository.findById(id).orElseThrow();
        entity.setOccupancyStatus(OccupancyStatus.NO_SHOW);
        bookingRepository.saveAndFlush(entity);

        Booking noShow = bookingService.getById(id);
        assertThat(noShow.getBalanceDue()).isEqualTo("0.00");
        assertThat(noShow.getOwes()).isFalse();
        assertThat(unpaidIds()).doesNotContain(id);
        // Once labelled, it leaves the "not arrived" block.
        assertThat(nightAuditService.get(TODAY.toString()).getNotArrived().stream().map(b -> b.getId())).doesNotContain(id);
    }

    @Test
    void aStayThatIsOverAndNeverArrivedFor_owesNothingForTheRoom_butOtherChargesStill() {
        BigDecimal stay = new BigDecimal("3000.00");
        assertThat(new FolioBreakdown(stay, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, true).balanceDue())
                .isEqualByComparingTo("0.00");
        assertThat(new FolioBreakdown(stay, BigDecimal.ZERO, new BigDecimal("450.00"), 1, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, true).balanceDue())
                .isEqualByComparingTo("450.00");
        // The same stay still ahead of us (or one in the house) is not waived.
        assertThat(new FolioBreakdown(stay, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BookingStatus.CONFIRMED, OccupancyStatus.EXPECTED, false).balanceDue())
                .isEqualByComparingTo("3000.00");

        BookingEntity past = booking(BookingStatus.NEW, OccupancyStatus.EXPECTED, TODAY.minusDays(5), TODAY.minusDays(3));
        assertThat(BookingFolioCalculator.neverArrivedAndOver(past, TODAY)).isTrue();
        assertThat(BookingFolioCalculator.neverArrivedAndOver(booking(BookingStatus.NEW, OccupancyStatus.EXPECTED, TODAY.minusDays(1), TODAY), TODAY)).as("departure day").isFalse();
        assertThat(BookingFolioCalculator.neverArrivedAndOver(booking(BookingStatus.CANCELLED, OccupancyStatus.EXPECTED, TODAY.minusDays(5), TODAY.minusDays(3)), TODAY)).isFalse();
        assertThat(BookingFolioCalculator.neverArrivedAndOver(booking(BookingStatus.NEW, OccupancyStatus.CHECKED_IN, TODAY.minusDays(5), TODAY.minusDays(3)), TODAY)).isFalse();
    }

    private java.util.List<String> unpaidIds() {
        NightAudit audit = nightAuditService.get(TODAY.toString());
        return audit.getUnpaidBalances().stream().map(b -> b.getId()).toList();
    }
}
