package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.CheckOutInput;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.CheckOutPreview;
import com.sunsetbeach.model.CheckOutResult;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.sunsetbeach.support.FixedHotelClockConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * An early checkout that shortens the stay - {@link BookingOccupancyService#checkOut} with
 * {@code shortenStay}. The desk's case: a two-night stay whose guest left on the first day kept
 * the room sold, the nights in the night audit and the full price in lifetime value, because
 * checking out never touched the dates. "Today" is pinned to 2034-03-10, midday Bangkok.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class EarlyCheckOutTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private RoomEntity room;
    private RoomUnitEntity unit;
    private RoomUnitEntity otherUnit;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Early Checkout Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by EarlyCheckOutTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("3500.00"));
        room = roomRepository.saveAndFlush(newRoom);

        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("Early-" + UUID.randomUUID().toString().substring(0, 8));
        newUnit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(newUnit);

        RoomUnitEntity second = new RoomUnitEntity();
        second.setRoomId(room.getId());
        second.setLabel("Early2-" + UUID.randomUUID().toString().substring(0, 8));
        second.setActive(true);
        otherUnit = roomUnitRepository.saveAndFlush(second);
    }

    private Booking checkedInStay(LocalDate checkIn, LocalDate checkOut) {
        // Staff creation: the public form refuses a check-in in the past, and a stay that already started is back-entry.
        Booking booking = bookingService.createStaffBooking(new com.sunsetbeach.model.StaffBookingCreateInput(
                room.getId(), "Early Guest", checkIn.toString(), checkOut.toString(), com.sunsetbeach.model.BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        occupancyService.checkIn(booking.getId());
        return bookingService.getById(booking.getId());
    }

    @Test
    void leavingOnTheArrivalDay_shortensToOneNight_andReleasesTheRest() {
        Booking booking = checkedInStay(TODAY, TODAY.plusDays(2));
        BigDecimal fullPrice = new BigDecimal(booking.getTotalPrice());
        BigDecimal oneNight = fullPrice.divide(BigDecimal.valueOf(2));

        CheckOutPreview preview = occupancyService.previewCheckOut(booking.getId());
        assertThat(preview.getEarly()).isTrue();
        assertThat(preview.getShortenable()).isTrue();
        assertThat(preview.getShortenedCheckOut().get()).isEqualTo(TODAY.plusDays(1));
        assertThat(preview.getNightsReleased()).isEqualTo(1);
        assertThat(new BigDecimal(preview.getUnusedNightsAmount())).isEqualByComparingTo(oneNight);
        // The dialog shows one balance per choice, both from the server: full stay vs. nights stayed.
        assertThat(new BigDecimal(preview.getOutstandingBalance())).isEqualByComparingTo(fullPrice);
        assertThat(new BigDecimal(preview.getShortenedOutstandingBalance())).isEqualByComparingTo(oneNight);

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        assertThat(result.getBooking().getCheckOut()).isEqualTo(TODAY.plusDays(1).toString());
        assertThat(result.getBooking().getSegments()).singleElement()
                .satisfies(s -> assertThat(s.getCheckOut()).isEqualTo(TODAY.plusDays(1).toString()));
        assertThat(new BigDecimal(result.getBooking().getTotalPrice())).isEqualByComparingTo(oneNight);
        assertThat(new BigDecimal(result.getOutstandingBalance())).isEqualByComparingTo(oneNight);

        // The released night is sellable again - the same unit takes a new booking for it.
        Booking next = bookingService.createBooking(new BookingCreateInput(
                room.getId(), "Next Guest", "next@example.com", "+66800000001", TODAY.plusDays(1).toString(), TODAY.plusDays(2).toString(), 1));
        bookingService.assignRoomUnit(next.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
    }

    @Test
    void chargingTheUnusedNights_keepsTheirPriceAsAnEarlyDepartureFee() {
        Booking booking = checkedInStay(TODAY.minusDays(2), TODAY.plusDays(2));
        BigDecimal fullPrice = new BigDecimal(booking.getTotalPrice());

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(true));

        assertThat(result.getBooking().getCheckOut()).isEqualTo(TODAY.toString());
        BigDecimal stayed = new BigDecimal(result.getBooking().getTotalPrice());
        BigDecimal fee = new BigDecimal(result.getBooking().getEarlyDepartureFee());
        assertThat(stayed).isEqualByComparingTo(fullPrice.divide(BigDecimal.valueOf(2)));
        assertThat(stayed.add(fee)).isEqualByComparingTo(fullPrice);

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(new BigDecimal(folio.getEarlyDepartureFee())).isEqualByComparingTo(fee);
        assertThat(new BigDecimal(folio.getBalanceDue())).isEqualByComparingTo(fullPrice);
    }

    /** The stay moved to {@code otherUnit} from {@code moveOn} on; the guest is checked in, in {@code unit}, today. */
    private Booking relocatedStay(LocalDate checkIn, LocalDate checkOut, LocalDate moveOn) {
        Booking booking = checkedInStay(checkIn, checkOut);
        bookingService.relocate(booking.getId(), new RelocationInput(moveOn.toString(), room.getId()).roomUnitId(otherUnit.getId()));
        return bookingService.getById(booking.getId());
    }

    @Test
    void leavingBeforeAFutureRoomMove_dropsTheNewRoom_andFreesIt() {
        // Room 1 for the first three nights, room 2 for the last two - the guest leaves today, the
        // third night's morning, so the move never happens.
        Booking booking = relocatedStay(TODAY.minusDays(2), TODAY.plusDays(3), TODAY.plusDays(1));
        assertThat(booking.getSegments()).hasSize(2);

        CheckOutPreview preview = occupancyService.previewCheckOut(booking.getId());
        assertThat(preview.getShortenable()).isTrue();
        assertThat(preview.getReason().orElse(null)).isNull();
        assertThat(preview.getNightsReleased()).isEqualTo(3);

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        Booking out = result.getBooking();
        assertThat(out.getCheckOut()).isEqualTo(TODAY.toString());
        assertThat(out.getSegments()).singleElement().satisfies(s -> {
            assertThat(s.getCheckIn()).isEqualTo(TODAY.minusDays(2).toString());
            assertThat(s.getCheckOut()).isEqualTo(TODAY.toString());
        });
        // Two nights stayed, at the 3500 the booking was agreed at.
        assertThat(new BigDecimal(out.getTotalPrice())).isEqualByComparingTo("7000.00");
        assertThat(out.getRoomUnitId().orElse(null)).isEqualTo(unit.getId());

        // The room the guest never moved into is sellable again for the nights it was held.
        Booking next = bookingService.createBooking(new BookingCreateInput(
                room.getId(), "Next Guest", "next@example.com", "+66800000002", TODAY.plusDays(1).toString(), TODAY.plusDays(3).toString(), 1));
        bookingService.assignRoomUnit(next.getId(), new RoomUnitAssignmentInput().roomUnitId(otherUnit.getId()));
    }

    @Test
    void leavingOnTheDayOfTheRoomMove_dropsTheNewRoom_andKeepsTheOldOneAsIs() {
        Booking booking = relocatedStay(TODAY.minusDays(2), TODAY.plusDays(2), TODAY);

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(true));

        Booking out = result.getBooking();
        assertThat(out.getCheckOut()).isEqualTo(TODAY.toString());
        assertThat(out.getSegments()).singleElement().satisfies(s -> assertThat(s.getCheckOut()).isEqualTo(TODAY.toString()));
        assertThat(new BigDecimal(out.getTotalPrice())).isEqualByComparingTo("7000.00");
        // The two nights that would have been spent in room 2 are the early-departure fee.
        assertThat(new BigDecimal(out.getEarlyDepartureFee())).isEqualByComparingTo("7000.00");
        assertThat(out.getRoomUnitId().orElse(null)).isEqualTo(unit.getId());
    }

    @Test
    void leavingAfterTheRoomMove_shortensTheLastRoom() {
        Booking booking = relocatedStay(TODAY.minusDays(3), TODAY.plusDays(3), TODAY.minusDays(1));

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        Booking out = result.getBooking();
        assertThat(out.getCheckOut()).isEqualTo(TODAY.toString());
        assertThat(out.getSegments()).hasSize(2);
        assertThat(out.getSegments().get(1).getCheckIn()).isEqualTo(TODAY.minusDays(1).toString());
        assertThat(out.getSegments().get(1).getCheckOut()).isEqualTo(TODAY.toString());
        // Two nights in room 1 and one in room 2.
        assertThat(new BigDecimal(out.getTotalPrice())).isEqualByComparingTo("10500.00");
        assertThat(out.getRoomUnitId().orElse(null)).isEqualTo(otherUnit.getId());
        // Check-out marks the room of the last night only; the first room was marked when the guest moved out of it.
        assertThat(roomUnitRepository.findById(otherUnit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void theRoomTheGuestMovedOutOf_isMarkedDirtyAtTheMove_andLeftAloneAtAnEarlyCheckout() {
        // Moved from room 1 to room 2 yesterday; the guest is in the house.
        Booking booking = relocatedStay(TODAY.minusDays(3), TODAY.plusDays(3), TODAY.minusDays(1));
        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus())
                .as("marked dirty the moment the guest moved out").isEqualTo(HousekeepingStatus.DIRTY);

        // Housekeeping cleaned it that same day.
        RoomUnitEntity cleaned = roomUnitRepository.findById(unit.getId()).orElseThrow();
        cleaned.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        roomUnitRepository.saveAndFlush(cleaned);

        occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus())
                .as("an early check-out does not touch the room the guest left earlier").isEqualTo(HousekeepingStatus.CLEAN);
        assertThat(roomUnitRepository.findById(otherUnit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void movedToday_thenLeftEarlyToday_bothTheOldAndTheCurrentRoomAreDirty() {
        // 201 -> 202 took effect today; the guest is in 202 when they leave early, and the shortened stay forgets 202.
        Booking booking = relocatedStay(TODAY.minusDays(2), TODAY.plusDays(3), TODAY);
        RoomUnitEntity oldRoom = roomUnitRepository.findById(unit.getId()).orElseThrow();
        assertThat(oldRoom.getHousekeepingStatus()).as("marked at the move").isEqualTo(HousekeepingStatus.DIRTY);

        CheckOutResult result = occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        assertThat(result.getBooking().getRoomUnitId().orElse(null)).isEqualTo(unit.getId());
        assertThat(roomUnitRepository.findById(otherUnit.getId()).orElseThrow().getHousekeepingStatus())
                .as("the room the guest was in at the moment of departure").isEqualTo(HousekeepingStatus.DIRTY);
        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void aMoveThatIsStillAhead_doesNotMarkTheCurrentRoomDirty() {
        relocatedStay(TODAY.minusDays(2), TODAY.plusDays(3), TODAY.plusDays(1));

        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isNotEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void leavingBeforeAFutureRoomMove_leavesTheNeverUsedRoomAlone() {
        Booking booking = relocatedStay(TODAY.minusDays(2), TODAY.plusDays(3), TODAY.plusDays(1));

        occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
        assertThat(roomUnitRepository.findById(otherUnit.getId()).orElseThrow().getHousekeepingStatus()).isNotEqualTo(HousekeepingStatus.DIRTY);
    }

    @Test
    void withoutShortening_theDatesAndPriceStayAsTheyWere() {
        Booking booking = checkedInStay(TODAY, TODAY.plusDays(2));

        CheckOutResult result = occupancyService.checkOut(booking.getId(), null);

        assertThat(result.getBooking().getCheckOut()).isEqualTo(TODAY.plusDays(2).toString());
        assertThat(result.getBooking().getTotalPrice()).isEqualTo(booking.getTotalPrice());
    }

    @Test
    void aOneNightStayLeavingOnItsArrivalDay_isNotEarly() {
        Booking booking = checkedInStay(TODAY, TODAY.plusDays(1));

        CheckOutPreview preview = occupancyService.previewCheckOut(booking.getId());

        assertThat(preview.getEarly()).isFalse();
        assertThat(preview.getNightsReleased()).isZero();
    }
}
