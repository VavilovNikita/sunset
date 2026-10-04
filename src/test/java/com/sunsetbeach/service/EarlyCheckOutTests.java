package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.CheckOutInput;
import com.sunsetbeach.model.CheckOutPreview;
import com.sunsetbeach.model.CheckOutResult;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * An early checkout that shortens the stay - {@link BookingOccupancyService#checkOut} with
 * {@code shortenStay}. The desk's case: a two-night stay whose guest left on the first day kept
 * the room sold, the nights in the night audit and the full price in lifetime value, because
 * checking out never touched the dates. "Today" is pinned to 2034-03-10, midday Bangkok.
 */
@SpringBootTest
@Transactional
class EarlyCheckOutTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2034-03-10T12:00").atZone(BANGKOK).toInstant();
    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private RoomEntity room;
    private RoomUnitEntity unit;

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
    }

    private Booking checkedInStay(LocalDate checkIn, LocalDate checkOut) {
        Booking booking = bookingService.createBooking(
                new BookingCreateInput(room.getId(), "Early Guest", "guest@example.com", "+66800000000", checkIn.toString(), checkOut.toString(), 1));
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
