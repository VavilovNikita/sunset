package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.InHouseReport;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.ManagerReport;
import com.sunsetbeach.model.ManagerReportDay;
import com.sunsetbeach.model.MarketSegment;
import com.sunsetbeach.model.MarketSegmentRow;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /reports/in-house} (Z180) and {@code GET /reports/manager} (scoped Z370) against
 * hand-built fixtures in 2032/2033, clear of the baseline dump. Every test passes an explicit
 * date, so nothing here depends on the wall clock. {@code updatedAt}/{@code checkedOutAt} are
 * UTC wall-clock, like {@link ReportDateRange} assumes: a Bangkok night D is
 * [D-1 17:00Z, D 17:00Z).
 */
@SpringBootTest
@Transactional
class ReportInHouseAndManagerTests extends AbstractIntegrationTest {

    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private RoomUnitBlockRepository blockRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private RoomUnitEntity r1;
    private RoomUnitEntity r2;
    private RoomUnitEntity r3;
    private RoomUnitEntity inactive;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("In-house Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by ReportInHouseAndManagerTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
        r1 = persistUnit("R1", true);
        r2 = persistUnit("R2", true);
        r3 = persistUnit("R3", true);
        inactive = persistUnit("R4", false);

        UserEntity newUser = new UserEntity();
        newUser.setEmail("inhouse-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.MANAGER);
        user = userRepository.saveAndFlush(newUser);
    }

    // --- In-house -----------------------------------------------------------------------------

    /**
     * Night 2033-12-10. Listed: A (checked in), E (checked out the next morning), G (relocated,
     * checked in - one row, tonight's segment, whole-stay dates). Not listed: B expected, C
     * departing that morning, D no-show, F checked out that morning, H cancelled.
     */
    @Test
    void inHouse_listsOnlyGuestsPresentThatNight_oneRowPerBooking() {
        BookingEntity a = persistStay(BookingChannel.AGODA, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_IN, r1, "2033-12-08", "2033-12-12", 2, 1);
        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.EXPECTED, r2, "2033-12-10", "2033-12-11", 1, 0);
        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_IN, r2, "2033-12-07", "2033-12-10", 1, 0);
        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.NO_SHOW, null, "2033-12-09", "2033-12-11", 1, 0);
        BookingEntity e = persistStay(BookingChannel.PHONE, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_OUT, r3, "2033-12-09", "2033-12-12", 1, 1);
        checkedOutAt(e, "2033-12-11T03:00"); // 10:00 Bangkok on the 11th - was here the night of the 10th
        BookingEntity f = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_OUT, r2, "2033-12-09", "2033-12-12", 1, 0);
        checkedOutAt(f, "2033-12-10T03:00"); // 10:00 Bangkok on the 10th - left before that night

        BookingEntity g = persistBooking(BookingChannel.DIRECT, BookingPurpose.COMPLIMENTARY, OccupancyStatus.CHECKED_IN, "2033-12-08", "2033-12-13", 2, 0);
        persistSegment(g, r2, "2033-12-08", "2033-12-10", "2000.00");
        persistSegment(g, null, "2033-12-10", "2033-12-13", "3000.00");

        BookingEntity h = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_IN, r3, "2033-12-10", "2033-12-11", 1, 0);
        h.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(h);

        InHouseReport report = reportService.inHouse("2033-12-10");

        assertThat(report.getDate()).isEqualTo("2033-12-10");
        assertThat(report.getRooms()).extracting(InHouseRow::getBookingId).containsExactly(a.getId(), e.getId(), g.getId());

        InHouseRow rowA = report.getRooms().get(0);
        assertThat(rowA.getRoomName()).isEqualTo(room.getName());
        assertThat(rowA.getRoomUnitLabel().get()).isEqualTo(r1.getLabel());
        assertThat(rowA.getAdults()).isEqualTo(2);
        assertThat(rowA.getChildren()).isEqualTo(1);
        assertThat(rowA.getMarketSegment()).isEqualTo(MarketSegment.OTA);
        assertThat(rowA.getArrival()).isEqualTo("2033-12-08");
        assertThat(rowA.getDeparture()).isEqualTo("2033-12-12");

        InHouseRow rowG = report.getRooms().get(2);
        assertThat(rowG.getRoomUnitLabel().get()).isNull(); // tonight's segment, not the earlier one in R2
        assertThat(rowG.getMarketSegment()).isEqualTo(MarketSegment.COM);
        assertThat(rowG.getArrival()).isEqualTo("2033-12-08");
        assertThat(rowG.getDeparture()).isEqualTo("2033-12-13");

        assertThat(report.getTotal().getRooms()).isEqualTo(3);
        assertThat(report.getTotal().getAdults()).isEqualTo(2 + 1 + 2);
        assertThat(report.getTotal().getChildren()).isEqualTo(1 + 1);
    }

    /** The in-house column and the market-segment report put the same booking in the same bucket. */
    @Test
    void inHouse_marketSegment_matchesMarketSegmentReportForThatNight() {
        persistStay(BookingChannel.WALK_IN, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_IN, r1, "2033-12-20", "2033-12-22", 1, 0);

        InHouseRow row = reportService.inHouse("2033-12-20").getRooms().get(0);
        List<MarketSegmentRow> segments = reportService.marketSegment("2033-12-20", "2033-12-20").getSegments();

        assertThat(row.getMarketSegment()).isEqualTo(MarketSegment.WLK);
        for (MarketSegmentRow segment : segments) {
            assertThat(segment.getRoomNights()).isEqualTo(segment.getSegment() == row.getMarketSegment() ? 1 : 0);
        }
    }

    @Test
    void singleNightReports_invalidDate_isValidationError() {
        assertThatThrownBy(() -> reportService.inHouse("2033-02-30")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> reportService.manager("2033-02-30")).isInstanceOf(ValidationException.class);
    }

    // --- Manager report -----------------------------------------------------------------------

    /**
     * Night N = 2033-11-10. R3 is blocked (twice, overlapping) and so is the inactive R4, which
     * isn't inventory at all: out of order = 1.
     * <ul>
     *   <li>A: standard, checked in, R1, 11-08 -> 11-12, ฿4000 (฿1000 tonight), 2 adults + 1 child.
     *   <li>B: complimentary, checked in, R2, 11-10 -> 11-11, ฿0, 1 adult. Arrival.
     *   <li>C: house use, expected, unassigned, 11-10 -> 11-13, ฿1500 (฿500 tonight), walk-in, 2 adults. Arrival.
     *   <li>D: cancelled on N (updatedAt 05:00Z). Not occupied, not an arrival.
     *   <li>D2: cancelled at 18:00Z = 01:00 Bangkok on N+1. Not a cancellation for N.
     *   <li>E: no-show marked on N, 11-10 -> 11-12, ฿2000 (฿1000 tonight). Still occupies the room. Arrival.
     *   <li>F: checked out, departs N.
     *   <li>G: arrives N+1, 11-11 -> 11-13.
     * </ul>
     * Occupied 4 (A, B, C, E), comp 1, house use 1, without both 2; revenue ฿2500, ADR ฿625.
     * In house A + B: 2 rooms, 3 adults, 1 child, 4 guests, 2.00 per room, in-house revenue ฿1000
     * -> ฿250 per guest, length of stay (4 + 1) / 2 = 2.50, 1 comp guest. Revenue per in-house
     * guest ฿2500 / 4 = ฿625. Tomorrow: G arrives, B departs, occupied A, C, E, G = 4.
     */
    @Test
    void manager_matchesHandComputedFigures() {
        persistBlock(r3, "2033-11-09", "2033-11-11");
        persistBlock(r3, "2033-11-10", "2033-11-10");
        persistBlock(inactive, "2033-11-01", "2033-11-30");

        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_IN, r1, "2033-11-08", "2033-11-12", 2, 1, "4000.00");
        persistStay(BookingChannel.PHONE, BookingPurpose.COMPLIMENTARY, OccupancyStatus.CHECKED_IN, r2, "2033-11-10", "2033-11-11", 1, 0, "0.00");
        persistStay(BookingChannel.WALK_IN, BookingPurpose.HOUSE_USE, OccupancyStatus.EXPECTED, null, "2033-11-10", "2033-11-13", 2, 0, "1500.00");
        BookingEntity d = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.EXPECTED, r2, "2033-11-10", "2033-11-11", 1, 0, "900.00");
        BookingEntity d2 = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.EXPECTED, r2, "2033-11-10", "2033-11-11", 1, 0, "900.00");
        BookingEntity e = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.NO_SHOW, null, "2033-11-10", "2033-11-12", 1, 0, "2000.00");
        BookingEntity f = persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_OUT, r2, "2033-11-07", "2033-11-10", 1, 0, "3000.00");
        checkedOutAt(f, "2033-11-10T04:00");
        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.EXPECTED, r2, "2033-11-11", "2033-11-13", 1, 0, "2000.00");
        cancel(d);
        cancel(d2);
        updatedAt(d, "2033-11-10T05:00");
        updatedAt(d2, "2033-11-10T18:00");
        updatedAt(e, "2033-11-10T12:00");

        // Last year's night, for the lastYear column only.
        persistStay(BookingChannel.DIRECT, BookingPurpose.STANDARD, OccupancyStatus.CHECKED_OUT, r1, "2032-11-10", "2032-11-11", 1, 0, "800.00");

        ManagerReport report = reportService.manager("2033-11-10");
        ManagerReportDay today = report.getToday();
        int totalRooms = activeUnitCount();
        int available = totalRooms - 1;

        assertThat(report.getDate()).isEqualTo("2033-11-10");
        assertThat(report.getLastYearDate()).isEqualTo("2032-11-10");

        assertThat(today.getRooms().getTotalRooms()).isEqualTo(totalRooms);
        assertThat(today.getRooms().getOutOfOrder()).isEqualTo(1);
        assertThat(today.getRooms().getAvailableForSale()).isEqualTo(available);
        assertThat(today.getRooms().getOccupied()).isEqualTo(4);
        assertThat(today.getRooms().getOccupied())
                .isEqualTo(reportService.occupancy("2033-11-10", "2033-11-10").getTotal().getRoomNightsSold());
        assertThat(today.getRooms().getComplimentary()).isEqualTo(1);
        assertThat(today.getRooms().getHouseUse()).isEqualTo(1);
        assertThat(today.getRooms().getOccupiedExcludingCompAndHouseUse()).isEqualTo(2);
        assertThat(today.getRooms().getOccupancyPercent().get()).isEqualTo(ratio(400, available));
        assertThat(today.getRooms().getAverageRatePerOccupiedRoom().get()).isEqualTo("625.00");
        assertThat(today.getRooms().getAverageRevenuePerAvailableRoom().get()).isEqualTo(ratio(2500, available));

        InHouseReport inHouse = reportService.inHouse("2033-11-10");
        assertThat(today.getGuests().getAdultsInHouse()).isEqualTo(3).isEqualTo(inHouse.getTotal().getAdults());
        assertThat(today.getGuests().getChildrenInHouse()).isEqualTo(1).isEqualTo(inHouse.getTotal().getChildren());
        assertThat(today.getGuests().getGuestsInHouse()).isEqualTo(4);
        assertThat(today.getGuests().getAverageGuestsPerRoom().get()).isEqualTo("2.00");
        assertThat(today.getGuests().getAverageRatePerGuest().get()).isEqualTo("250.00");
        assertThat(today.getGuests().getAverageLengthOfStay().get()).isEqualTo("2.50");
        assertThat(today.getGuests().getComplimentaryGuests()).isEqualTo(1);
        assertThat(today.getGuests().getHouseUseGuests()).isZero(); // C is house use but hasn't arrived

        assertThat(today.getAccounts().getArrivals()).isEqualTo(3);
        assertThat(today.getAccounts().getDepartures()).isEqualTo(1);
        assertThat(today.getAccounts().getCancellations()).isEqualTo(1);
        assertThat(today.getAccounts().getNoShows()).isEqualTo(1);
        assertThat(today.getAccounts().getWalkInRooms()).isEqualTo(1);

        assertThat(today.getRevenue().getRoomRevenue()).isEqualTo("2500.00");
        assertThat(today.getRevenue().getAverageRevenuePerInHouseGuest().get()).isEqualTo("625.00");

        assertThat(today.getTomorrow().getDate()).isEqualTo("2033-11-11");
        assertThat(today.getTomorrow().getArrivals()).isEqualTo(1);
        assertThat(today.getTomorrow().getDepartures()).isEqualTo(1);
        assertThat(today.getTomorrow().getOccupied()).isEqualTo(4);
        assertThat(today.getTomorrow().getAvailableForSale()).isEqualTo(available); // R3's first block runs through the 11th
        assertThat(today.getTomorrow().getOccupancyPercent().get()).isEqualTo(ratio(400, available));

        ManagerReportDay lastYear = report.getLastYear();
        assertThat(lastYear.getRooms().getOccupied()).isEqualTo(1);
        assertThat(lastYear.getRooms().getOutOfOrder()).isZero();
        assertThat(lastYear.getRevenue().getRoomRevenue()).isEqualTo("800.00");
        assertThat(lastYear.getGuests().getGuestsInHouse()).isZero(); // checked out, checkedOutAt never set
    }

    /** A night with nothing sold: every ratio with a zero denominator is null, not a division error. */
    @Test
    void manager_emptyNight_hasNullRatios() {
        ManagerReportDay day = reportService.manager("2033-01-15").getToday();

        assertThat(day.getRooms().getOccupied()).isZero();
        assertThat(day.getRooms().getAverageRatePerOccupiedRoom().get()).isNull();
        assertThat(day.getGuests().getAverageGuestsPerRoom().get()).isNull();
        assertThat(day.getGuests().getAverageRatePerGuest().get()).isNull();
        assertThat(day.getGuests().getAverageLengthOfStay().get()).isNull();
        assertThat(day.getRevenue().getRoomRevenue()).isEqualTo("0.00");
        assertThat(day.getRevenue().getAverageRevenuePerInHouseGuest().get()).isNull();
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private int activeUnitCount() {
        return (int) roomUnitRepository.findAll().stream().filter(RoomUnitEntity::isActive).count();
    }

    private static String ratio(long numerator, int denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP).toPlainString();
    }

    private RoomUnitEntity persistUnit(String label, boolean active) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel(label + "-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(active);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private void persistBlock(RoomUnitEntity unit, String from, String to) {
        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(LocalDate.parse(from));
        block.setToDate(LocalDate.parse(to));
        block.setReason("Report test");
        block.setCreatedByUserId(user.getId());
        blockRepository.saveAndFlush(block);
    }

    private BookingEntity persistStay(
            BookingChannel channel, BookingPurpose purpose, OccupancyStatus occupancy, RoomUnitEntity unit,
            String checkIn, String checkOut, int adults, int children) {
        return persistStay(channel, purpose, occupancy, unit, checkIn, checkOut, adults, children, "1000.00");
    }

    /** A booking with one segment covering the whole stay - the never-relocated case. */
    private BookingEntity persistStay(
            BookingChannel channel, BookingPurpose purpose, OccupancyStatus occupancy, RoomUnitEntity unit,
            String checkIn, String checkOut, int adults, int children, String totalPrice) {
        BookingEntity booking = persistBooking(channel, purpose, occupancy, checkIn, checkOut, adults, children);
        persistSegment(booking, unit, checkIn, checkOut, totalPrice);
        return booking;
    }

    private BookingEntity persistBooking(
            BookingChannel channel, BookingPurpose purpose, OccupancyStatus occupancy, String checkIn, String checkOut, int adults, int children) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(channel);
        booking.setPurpose(purpose);
        booking.setOccupancyStatus(occupancy);
        booking.setAdults(adults);
        booking.setChildren(children);
        booking.setRoomId(room.getId());
        booking.setGuestName("In-house Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.saveAndFlush(booking);
    }

    private void persistSegment(BookingEntity booking, RoomUnitEntity unit, String checkIn, String checkOut, String totalPrice) {
        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(booking.getId());
        segment.setRoomId(room.getId());
        segment.setRoomUnitId(unit != null ? unit.getId() : null);
        segment.setCheckIn(LocalDate.parse(checkIn));
        segment.setCheckOut(LocalDate.parse(checkOut));
        segment.setTotalPrice(new BigDecimal(totalPrice));
        segmentRepository.saveAndFlush(segment);
    }

    private void checkedOutAt(BookingEntity booking, String utc) {
        booking.setCheckedOutAt(LocalDateTime.parse(utc));
        bookingRepository.saveAndFlush(booking);
    }

    private void cancel(BookingEntity booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);
    }

    /** {@code @UpdateTimestamp} overwrites any value set through the entity - backdate natively, then drop the stale cache. */
    private void updatedAt(BookingEntity booking, String utc) {
        entityManager.createNativeQuery("UPDATE \"Booking\" SET \"updatedAt\" = ?1 WHERE id = ?2")
                .setParameter(1, LocalDateTime.parse(utc))
                .setParameter(2, booking.getId())
                .executeUpdate();
        entityManager.clear();
    }
}
