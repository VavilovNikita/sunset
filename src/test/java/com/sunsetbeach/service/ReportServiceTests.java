package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestLtvRow;
import com.sunsetbeach.model.MarketSegment;
import com.sunsetbeach.model.MarketSegmentReport;
import com.sunsetbeach.model.MarketSegmentRow;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyReport;
import com.sunsetbeach.model.OccupancyReportRow;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PosSalesMixCategory;
import com.sunsetbeach.model.PosSalesMixDepartment;
import com.sunsetbeach.model.PosSalesMixItem;
import com.sunsetbeach.model.PosSalesMixReport;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.TopProductionReport;
import com.sunsetbeach.model.TopProductionRow;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.ShiftRepository;
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
 * The three dashboard reports against small hand-built fixtures whose expected figures are
 * computed by hand in each test's comments. Far-future dates (2033) keep the fixtures out of
 * whatever the baseline dump contains; {@code @Transactional} rolls everything back, and
 * nothing here writes an audit row.
 */
@SpringBootTest
@Transactional
class ReportServiceTests extends AbstractIntegrationTest {

    @Autowired private ReportService reportService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private List<RoomUnitEntity> units;
    private UserEntity user;
    private ShiftEntity shift;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Report Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by ReportServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        // Two active units and one inactive one: the inactive unit must not count as inventory.
        units = List.of(persistUnit("R1", true), persistUnit("R2", true), persistUnit("R3", false));

        UserEntity newUser = new UserEntity();
        newUser.setEmail("reports-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.MANAGER);
        user = userRepository.saveAndFlush(newUser);

        ShiftEntity newShift = new ShiftEntity();
        newShift.setOpenedByUserId(user.getId());
        newShift.setStatus(ShiftStatus.CLOSED);
        shift = shiftRepository.saveAndFlush(newShift);
    }

    // --- Occupancy ----------------------------------------------------------------------------

    /**
     * Range 2033-07-10..12 = 3 nights × 2 active units = 6 available.
     * <ul>
     *   <li>A: 07-08 -> 07-11, ฿3000 (3 nights). In range: night 07-10 only -> 1 night, ฿1000.
     *   <li>B: 07-11 -> 07-14, ฿4500 (3 nights). In range: 07-11, 07-12 -> 2 nights, ฿3000.
     *   <li>C: 07-10 -> 07-11, ฿800, no unit assigned. 1 night, ฿800.
     *   <li>D: 07-10 -> 07-13, ฿5000, CANCELLED. Nothing.
     *   <li>E: 07-13 -> 07-15, starts the day after {@code to}. Nothing.
     * </ul>
     * Sold 4, revenue ฿4800, occupancy 66.67%, ADR ฿1200.00, RevPAR ฿800.00.
     */
    @Test
    void occupancy_matchesHandComputedFigures_withProrationCancellationAndUnassignedSegment() {
        persistStay(BookingStatus.CONFIRMED, units.get(0), "2033-07-08", "2033-07-11", "3000.00");
        persistStay(BookingStatus.PAID, units.get(1), "2033-07-11", "2033-07-14", "4500.00");
        persistStay(BookingStatus.NEW, null, "2033-07-10", "2033-07-11", "800.00");
        persistStay(BookingStatus.CANCELLED, units.get(1), "2033-07-10", "2033-07-13", "5000.00");
        persistStay(BookingStatus.CONFIRMED, units.get(0), "2033-07-13", "2033-07-15", "2000.00");

        OccupancyReport report = reportService.occupancy("2033-07-10", "2033-07-12");
        OccupancyReportRow row = rowFor(report);

        assertThat(report.getNights()).isEqualTo(3);
        assertThat(row.getActiveUnits()).isEqualTo(2);
        assertThat(row.getRoomNightsAvailable()).isEqualTo(6);
        assertThat(row.getRoomNightsSold()).isEqualTo(4);
        assertThat(row.getRoomRevenue()).isEqualTo("4800.00");
        assertThat(row.getOccupancyPercent().get()).isEqualTo("66.67");
        assertThat(row.getAdr().get()).isEqualTo("1200.00");
        assertThat(row.getRevpar().get()).isEqualTo("800.00");
    }

    /** ฿1000 over 3 nights, 1 in range: ฿333.333... - rounded once, on the way out. */
    @Test
    void occupancy_unevenProration_roundsOnlyTheFinalFigure() {
        persistStay(BookingStatus.CONFIRMED, units.get(0), "2033-08-01", "2033-08-04", "1000.00");
        persistStay(BookingStatus.CONFIRMED, units.get(1), "2033-08-01", "2033-08-04", "1000.00");

        OccupancyReportRow row = rowFor(reportService.occupancy("2033-08-03", "2033-08-03"));

        // Two segments at 333.333... each: 666.666... -> 666.67, not 333.33 + 333.33 = 666.66.
        assertThat(row.getRoomNightsSold()).isEqualTo(2);
        assertThat(row.getRoomRevenue()).isEqualTo("666.67");
        assertThat(row.getOccupancyPercent().get()).isEqualTo("100.00");
    }

    @Test
    void occupancy_revparEqualsAdrTimesOccupancy_andTotalSumsTheRows() {
        persistStay(BookingStatus.CONFIRMED, units.get(0), "2033-09-01", "2033-09-06", "5250.00");
        persistStay(BookingStatus.CONFIRMED, null, "2033-09-03", "2033-09-12", "12345.67");

        OccupancyReport report = reportService.occupancy("2033-09-02", "2033-09-08");
        for (OccupancyReportRow row : concat(report.getRooms(), report.getTotal())) {
            if (row.getRoomNightsSold() == 0 || row.getRoomNightsAvailable() == 0) continue;
            // Identity: RevPAR = ADR × sold / available. ADR is already rounded, so allow one cent.
            BigDecimal viaAdr = new BigDecimal(row.getAdr().get())
                    .multiply(BigDecimal.valueOf(row.getRoomNightsSold()))
                    .divide(BigDecimal.valueOf(row.getRoomNightsAvailable()), 2, RoundingMode.HALF_UP);
            assertThat(new BigDecimal(row.getRevpar().get()).subtract(viaAdr).abs()).isLessThanOrEqualTo(new BigDecimal("0.01"));
        }

        OccupancyReportRow total = report.getTotal();
        assertThat(total.getRoomId().get()).isNull();
        assertThat(total.getRoomNightsSold()).isEqualTo(report.getRooms().stream().mapToInt(OccupancyReportRow::getRoomNightsSold).sum());
        assertThat(total.getRoomNightsAvailable()).isEqualTo(report.getRooms().stream().mapToInt(OccupancyReportRow::getRoomNightsAvailable).sum());
        BigDecimal rowRevenue = report.getRooms().stream().map(r -> new BigDecimal(r.getRoomRevenue())).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Rows are each rounded; the total is rounded once from the unrounded sum, so allow a cent per row.
        assertThat(new BigDecimal(total.getRoomRevenue()).subtract(rowRevenue).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.01").multiply(BigDecimal.valueOf(report.getRooms().size())));

        // This room's own row, by hand: 7 nights × 2 units = 14 available.
        // Segment 1: 09-01..09-06 (5 nights, ฿5250), in range 09-02..09-05 -> 4 nights, ฿4200.
        // Segment 2: 09-03..09-12 (9 nights, ฿12345.67), in range 09-03..09-08 -> 6 nights, ฿8230.4466...
        OccupancyReportRow row = rowFor(report);
        assertThat(row.getRoomNightsSold()).isEqualTo(10);
        assertThat(row.getRoomRevenue()).isEqualTo("12430.45");
    }

    @Test
    void occupancy_roomWithNoActiveUnits_hasNullRatiosInsteadOfDividingByZero() {
        units.forEach(u -> u.setActive(false));
        roomUnitRepository.saveAllAndFlush(units);
        persistStay(BookingStatus.CONFIRMED, null, "2033-10-01", "2033-10-02", "900.00");

        OccupancyReportRow row = rowFor(reportService.occupancy("2033-10-01", "2033-10-01"));
        assertThat(row.getRoomNightsAvailable()).isZero();
        assertThat(row.getRoomNightsSold()).isEqualTo(1);
        assertThat(row.getOccupancyPercent().get()).isNull();
        assertThat(row.getRevpar().get()).isNull();
        assertThat(row.getAdr().get()).isEqualTo("900.00");
    }

    @Test
    void occupancy_fromAfterTo_isRejected() {
        assertThatThrownBy(() -> reportService.occupancy("2033-07-12", "2033-07-10")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> reportService.occupancy("2033-02-30", "2033-03-01")).isInstanceOf(ValidationException.class);
    }

    // --- Top production / market segment --------------------------------------------------------

    /**
     * Range 2034-03-10..12 (3 nights). Nothing else in the database is booked in 2034.
     * <ul>
     *   <li>A: EXPEDIA, standard, 2+1 guests, 03-10 -> 03-12, ฿3000. 2 nights, ฿3000.
     *   <li>B: PHONE, COMPLIMENTARY, 1+0, 03-10 -> 03-11, ฿0. 1 night.
     *   <li>C: BOOKING_COM, COMPLIMENTARY, 2+2, 03-11 -> 03-12, ฿500. 1 night, ฿500.
     *   <li>D: WALK_IN, HOUSE_USE, 1+0, 03-12 -> 03-13, ฿0. 1 night.
     *   <li>E: PHONE, standard, 2+0, 03-09 -> 03-11, ฿2000. In range: 03-10 only -> 1 night, ฿1000.
     *   <li>G: WALK_IN, standard, 3+0, relocated: 03-10 -> 03-11 ฿700 and 03-11 -> 03-12 ฿900. 2 nights, ฿1600, 3 guests once.
     *   <li>F: EXPEDIA, standard, 5+0, CANCELLED. Nothing.
     * </ul>
     * Total: 8 room-nights, ฿6100, 14 guests.
     */
    private void persistProducerFixture() {
        persistProducerStay(BookingChannel.EXPEDIA, BookingPurpose.STANDARD, 2, 1, BookingStatus.CONFIRMED, "2034-03-10", "2034-03-12", "3000.00");
        persistProducerStay(BookingChannel.PHONE, BookingPurpose.COMPLIMENTARY, 1, 0, BookingStatus.CONFIRMED, "2034-03-10", "2034-03-11", "0.00");
        persistProducerStay(BookingChannel.BOOKING_COM, BookingPurpose.COMPLIMENTARY, 2, 2, BookingStatus.NEW, "2034-03-11", "2034-03-12", "500.00");
        persistProducerStay(BookingChannel.WALK_IN, BookingPurpose.HOUSE_USE, 1, 0, BookingStatus.CONFIRMED, "2034-03-12", "2034-03-13", "0.00");
        persistProducerStay(BookingChannel.PHONE, BookingPurpose.STANDARD, 2, 0, BookingStatus.PAID, "2034-03-09", "2034-03-11", "2000.00");
        persistProducerStay(BookingChannel.EXPEDIA, BookingPurpose.STANDARD, 5, 0, BookingStatus.CANCELLED, "2034-03-10", "2034-03-12", "4000.00");

        BookingEntity relocated = persistBooking(null, BookingStatus.CONFIRMED, "2034-03-10", "2034-03-12", "1600.00");
        relocated.setChannel(BookingChannel.WALK_IN);
        relocated.setAdults(3);
        bookingRepository.saveAndFlush(relocated);
        persistSegment(relocated, units.get(0), "2034-03-10", "2034-03-11", "700.00");
        persistSegment(relocated, units.get(1), "2034-03-11", "2034-03-12", "900.00");
    }

    @Test
    void topProduction_groupsByChannel_compAndHouseUseOverrideChannel_rankedByRoomNights() {
        persistProducerFixture();

        TopProductionReport report = reportService.topProduction("2034-03-10", "2034-03-12");

        // Room-nights desc, then revenue desc: EXPEDIA(2, 3000), WALK_IN(2, 1600), COMPLIMENTARY(2, 500), PHONE(1, 1000), HOUSE_USE(1, 0).
        assertThat(report.getProducers()).extracting(r -> r.getProducer().get())
                .containsExactly("EXPEDIA", "WALK_IN", "COMPLIMENTARY", "PHONE", "HOUSE_USE");
        TopProductionRow expedia = report.getProducers().get(0);
        assertThat(expedia.getLabel().get()).isEqualTo("Expedia");
        assertThat(expedia.getRoomNights()).isEqualTo(2);
        assertThat(expedia.getRevenue()).isEqualTo("3000.00");
        assertThat(expedia.getRoomNightsPercent().get()).isEqualTo("25.00");
        assertThat(expedia.getRevenuePercent().get()).isEqualTo("49.18"); // 3000 / 6100
        assertThat(expedia.getAdr().get()).isEqualTo("1500.00");

        // B (phone) and C (Booking.com) are both comps: one row, neither under its channel.
        TopProductionRow comp = report.getProducers().get(2);
        assertThat(comp.getLabel().get()).isEqualTo("Complimentary");
        assertThat(comp.getRoomNights()).isEqualTo(2);
        assertThat(comp.getRevenue()).isEqualTo("500.00");
        assertThat(report.getProducers()).extracting(r -> r.getProducer().get()).doesNotContain("BOOKING_COM");
        assertThat(report.getProducers().get(3).getRoomNights()).isEqualTo(1); // PHONE: E only, B went to Complimentary
        assertThat(report.getProducers().get(3).getRevenue()).isEqualTo("1000.00");

        assertThat(report.getTotal().getProducer().get()).isNull();
        assertThat(report.getTotal().getRoomNights()).isEqualTo(8);
        assertThat(report.getTotal().getRevenue()).isEqualTo("6100.00");
        assertThat(report.getTotal().getRoomNightsPercent().get()).isEqualTo("100.00");

        // Same population as the occupancy report for the same range.
        OccupancyReport occupancy = reportService.occupancy("2034-03-10", "2034-03-12");
        assertThat(occupancy.getTotal().getRoomNightsSold()).isEqualTo(report.getTotal().getRoomNights());
        assertThat(occupancy.getTotal().getRoomRevenue()).isEqualTo(report.getTotal().getRevenue());
    }

    @Test
    void marketSegment_rollsProducersUp_andCountsEachBookingsGuestsOnce() {
        persistProducerFixture();

        MarketSegmentReport report = reportService.marketSegment("2034-03-10", "2034-03-12");

        assertThat(report.getSegments()).extracting(MarketSegmentRow::getSegment)
                .containsExactly(MarketSegment.COM, MarketSegment.DIR, MarketSegment.HFO, MarketSegment.OTA, MarketSegment.WLK);
        // segment: room-nights, guests, revenue, average rate
        assertSegment(report.getSegments().get(0), 2, 5, "500.00", "250.00"); // B + C
        assertSegment(report.getSegments().get(1), 1, 2, "1000.00", "1000.00"); // E
        assertSegment(report.getSegments().get(2), 1, 1, "0.00", "0.00"); // D
        assertSegment(report.getSegments().get(3), 2, 3, "3000.00", "1500.00"); // A - EXPEDIA rolls into OTA
        assertSegment(report.getSegments().get(4), 2, 3, "1600.00", "800.00"); // G - two segments, 3 guests counted once

        MarketSegmentRow ota = report.getSegments().get(3);
        assertThat(ota.getRoomNightsPercent().get()).isEqualTo("25.00");
        assertThat(ota.getGuestsPercent().get()).isEqualTo("21.43"); // 3 / 14
        assertThat(ota.getRevenuePercent().get()).isEqualTo("49.18");

        assertThat(report.getTotal().getSegment()).isNull();
        assertSegment(report.getTotal(), 8, 14, "6100.00", "762.50");

        // Guests equal adults + children summed by hand over the distinct non-cancelled bookings
        // with a segment night in the range - the same population the segments query returns.
        int handSum = segmentRepository
                .findByBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                        BookingStatus.CANCELLED, LocalDate.parse("2034-03-13"), LocalDate.parse("2034-03-10"))
                .stream().map(BookingSegmentEntity::getBookingId).distinct()
                .map(id -> bookingRepository.findById(id).orElseThrow())
                .mapToInt(b -> b.getAdults() + b.getChildren())
                .sum();
        assertThat(report.getTotal().getGuests()).isEqualTo(handSum);
    }

    @Test
    void marketSegment_emptyRange_returnsAllFiveSegmentsWithZerosAndNullRatios() {
        MarketSegmentReport report = reportService.marketSegment("2034-11-01", "2034-11-02");

        assertThat(report.getSegments()).hasSize(5);
        for (MarketSegmentRow row : concatSegments(report.getSegments(), report.getTotal())) {
            assertThat(row.getRoomNights()).isZero();
            assertThat(row.getGuests()).isZero();
            assertThat(row.getRevenue()).isEqualTo("0.00");
            assertThat(row.getRoomNightsPercent().get()).isNull();
            assertThat(row.getGuestsPercent().get()).isNull();
            assertThat(row.getRevenuePercent().get()).isNull();
            assertThat(row.getAverageRate().get()).isNull();
        }
        assertThat(reportService.topProduction("2034-11-01", "2034-11-02").getProducers()).isEmpty();
    }

    @Test
    void topProductionAndMarketSegment_fromAfterTo_isRejected() {
        assertThatThrownBy(() -> reportService.topProduction("2034-03-12", "2034-03-10")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> reportService.marketSegment("2034-03-12", "2034-03-10")).isInstanceOf(ValidationException.class);
    }

    private static void assertSegment(MarketSegmentRow row, int roomNights, int guests, String revenue, String averageRate) {
        assertThat(row.getRoomNights()).isEqualTo(roomNights);
        assertThat(row.getGuests()).isEqualTo(guests);
        assertThat(row.getRevenue()).isEqualTo(revenue);
        assertThat(row.getAverageRate().get()).isEqualTo(averageRate);
    }

    private static List<MarketSegmentRow> concatSegments(List<MarketSegmentRow> rows, MarketSegmentRow total) {
        List<MarketSegmentRow> all = new java.util.ArrayList<>(rows);
        all.add(total);
        return all;
    }

    private void persistProducerStay(
            BookingChannel channel,
            BookingPurpose purpose,
            int adults,
            int children,
            BookingStatus status,
            String checkIn,
            String checkOut,
            String totalPrice) {
        BookingEntity booking = persistBooking(null, status, checkIn, checkOut, totalPrice);
        booking.setChannel(channel);
        booking.setPurpose(purpose);
        booking.setAdults(adults);
        booking.setChildren(children);
        bookingRepository.saveAndFlush(booking);
        persistSegment(booking, units.get(0), checkIn, checkOut, totalPrice);
    }

    // --- POS sales mix ------------------------------------------------------------------------

    /**
     * Range 2033-03-10..11 (Bangkok) = [2033-03-09T17:00Z, 2033-03-11T17:00Z).
     * <ul>
     *   <li>Order 1, PAID, paid 03-09T17:00Z (00:00 Bangkok on the 10th - first included instant):
     *       2 × Curry ฿200, 1 × Beer ฿100.
     *   <li>Order 2, PAID by ROOM_CHARGE, paid 03-11T16:59Z: 1 × Curry at an older price ฿180,
     *       3 × Beer ฿100, 1 × Massage ฿1500.
     *   <li>Order 3, PAID, paid 03-11T17:00Z (00:00 Bangkok on the 12th): excluded.
     *   <li>Order 4, SENT, no payment: excluded.
     * </ul>
     * Curry 3 / ฿580, Beer 4 / ฿400, Massage 1 / ฿1500. Total 8 / ฿2480.
     */
    @Test
    void posSalesMix_countsOnlyPaidOrdersSettledInsideTheBangkokRange() {
        String mains = "Mains " + UUID.randomUUID();
        String drinks = "Drinks " + UUID.randomUUID();
        MenuItemEntity curry = persistMenuItem("Curry", mains, MenuDepartment.KITCHEN, "200.00");
        MenuItemEntity beer = persistMenuItem("Beer", drinks, MenuDepartment.BAR, "100.00");
        MenuItemEntity massage = persistMenuItem("Massage", "Treatments " + UUID.randomUUID(), MenuDepartment.SPA, "1500.00");

        OrderEntity o1 = persistOrder(OrderStatus.PAID, line(curry, 2, "200.00"), line(beer, 1, "100.00"));
        persistPayment(o1, PaymentMethod.CASH, LocalDateTime.of(2033, 3, 9, 17, 0));
        OrderEntity o2 = persistOrder(OrderStatus.PAID, line(curry, 1, "180.00"), line(beer, 3, "100.00"), line(massage, 1, "1500.00"));
        persistPayment(o2, PaymentMethod.ROOM_CHARGE, LocalDateTime.of(2033, 3, 11, 16, 59));
        OrderEntity o3 = persistOrder(OrderStatus.PAID, line(curry, 10, "200.00"));
        persistPayment(o3, PaymentMethod.CARD, LocalDateTime.of(2033, 3, 11, 17, 0));
        persistOrder(OrderStatus.SENT, line(beer, 10, "100.00"));

        PosSalesMixReport report = reportService.posSalesMix("2033-03-10", "2033-03-11");

        assertThat(report.getItems()).extracting(PosSalesMixItem::getName, PosSalesMixItem::getQuantity, PosSalesMixItem::getRevenue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Massage", 1, "1500.00"),
                        org.assertj.core.groups.Tuple.tuple("Curry", 3, "580.00"),
                        org.assertj.core.groups.Tuple.tuple("Beer", 4, "400.00"));
        assertThat(report.getTotalQuantity()).isEqualTo(8);
        assertThat(report.getTotalRevenue()).isEqualTo("2480.00");

        assertThat(report.getCategories()).extracting(PosSalesMixCategory::getCategory)
                .containsExactly(massage.getCategory(), mains, drinks);
        assertThat(report.getDepartments()).extracting(PosSalesMixDepartment::getDepartment, PosSalesMixDepartment::getRevenue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(MenuDepartment.SPA, "1500.00"),
                        org.assertj.core.groups.Tuple.tuple(MenuDepartment.KITCHEN, "580.00"),
                        org.assertj.core.groups.Tuple.tuple(MenuDepartment.BAR, "400.00"));

        // Each rollup sums back to the per-item total.
        assertThat(report.getCategories().stream().mapToInt(PosSalesMixCategory::getQuantity).sum()).isEqualTo(report.getTotalQuantity());
        assertThat(report.getDepartments().stream().mapToInt(PosSalesMixDepartment::getQuantity).sum()).isEqualTo(report.getTotalQuantity());
        assertThat(sum(report.getCategories().stream().map(PosSalesMixCategory::getRevenue).toList())).isEqualByComparingTo(report.getTotalRevenue());
        assertThat(sum(report.getDepartments().stream().map(PosSalesMixDepartment::getRevenue).toList())).isEqualByComparingTo(report.getTotalRevenue());
        assertThat(sum(report.getItems().stream().map(PosSalesMixItem::getRevenue).toList())).isEqualByComparingTo(report.getTotalRevenue());
    }

    // --- Guest LTV ----------------------------------------------------------------------------

    /**
     * Amounts are large enough (tens of millions) that these guests outrank anything in the
     * baseline dump, so the top of the list is this fixture alone.
     * <ul>
     *   <li>Alice: 40M (3 nights, one segment) + 30M (4 nights, two segments - a relocation) = 70M,
     *       7 nights, 2 bookings; plus a CANCELLED 99M that must not count. Room charges: 450 + 50
     *       on her live bookings; 999 on the cancelled one doesn't count.
     *   <li>Bob: 60M, 2 nights, 1 booking, no room charges.
     *   <li>Carol: only a CANCELLED 99M booking - absent.
     * </ul>
     */
    @Test
    void guestLtv_ranksByRoomRevenue_andIgnoresCancelledBookings() {
        GuestEntity alice = persistGuest("Alice LTV");
        GuestEntity bob = persistGuest("Bob LTV");
        GuestEntity carol = persistGuest("Carol LTV");

        BookingEntity a1 = persistStayFor(alice, BookingStatus.PAID, "2033-05-01", "2033-05-04", "40000000.00");
        BookingEntity a2 = persistBooking(alice, BookingStatus.CONFIRMED, "2033-06-01", "2033-06-05", "30000000.00");
        persistSegment(a2, units.get(0), "2033-06-01", "2033-06-03", "15000000.00");
        persistSegment(a2, units.get(1), "2033-06-03", "2033-06-05", "15000000.00");
        BookingEntity aCancelled = persistStayFor(alice, BookingStatus.CANCELLED, "2033-07-01", "2033-07-02", "99000000.00");
        persistStayFor(bob, BookingStatus.NEW, "2033-04-01", "2033-04-03", "60000000.00");
        persistStayFor(carol, BookingStatus.CANCELLED, "2033-04-01", "2033-04-03", "99000000.00");

        persistPayment(persistOrder(OrderStatus.PAID), PaymentMethod.ROOM_CHARGE, a1, "450.00");
        persistPayment(persistOrder(OrderStatus.PAID), PaymentMethod.ROOM_CHARGE, a2, "50.00");
        persistPayment(persistOrder(OrderStatus.PAID), PaymentMethod.ROOM_CHARGE, aCancelled, "999.00");

        List<GuestLtvRow> rows = reportService.guestLtv(2).getGuests();

        assertThat(rows).extracting(GuestLtvRow::getGuestId).containsExactly(alice.getId(), bob.getId());
        GuestLtvRow a = rows.get(0);
        assertThat(a.getName()).isEqualTo("Alice LTV");
        assertThat(a.getBookingCount()).isEqualTo(2);
        assertThat(a.getTotalNights()).isEqualTo(7);
        assertThat(a.getRoomRevenue()).isEqualTo("70000000.00");
        assertThat(a.getRoomChargesTotal()).isEqualTo("500.00");
        assertThat(a.getFirstCheckIn()).isEqualTo("2033-05-01");
        assertThat(a.getLastCheckIn()).isEqualTo("2033-06-01");

        GuestLtvRow b = rows.get(1);
        assertThat(b.getBookingCount()).isEqualTo(1);
        assertThat(b.getTotalNights()).isEqualTo(2);
        assertThat(b.getRoomRevenue()).isEqualTo("60000000.00");
        assertThat(b.getRoomChargesTotal()).isEqualTo("0.00");

        assertThat(reportService.guestLtv(200).getGuests()).extracting(GuestLtvRow::getGuestId).doesNotContain(carol.getId());
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private RoomUnitEntity persistUnit(String label, boolean active) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel(label + "-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(active);
        return roomUnitRepository.saveAndFlush(unit);
    }

    private BookingEntity persistBooking(GuestEntity guest, BookingStatus status, String checkIn, String checkOut, String totalPrice) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setGuestName(guest != null ? guest.getName() : "Report Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        if (guest != null) booking.setGuestId(guest.getId());
        booking.setCheckIn(LocalDate.parse(checkIn));
        booking.setCheckOut(LocalDate.parse(checkOut));
        booking.setTotalPrice(new BigDecimal(totalPrice));
        booking.setStatus(status);
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

    /** A booking with one segment covering the whole stay - the never-relocated case. */
    private BookingEntity persistStay(BookingStatus status, RoomUnitEntity unit, String checkIn, String checkOut, String totalPrice) {
        BookingEntity booking = persistBooking(null, status, checkIn, checkOut, totalPrice);
        persistSegment(booking, unit, checkIn, checkOut, totalPrice);
        return booking;
    }

    private BookingEntity persistStayFor(GuestEntity guest, BookingStatus status, String checkIn, String checkOut, String totalPrice) {
        BookingEntity booking = persistBooking(guest, status, checkIn, checkOut, totalPrice);
        persistSegment(booking, units.get(0), checkIn, checkOut, totalPrice);
        return booking;
    }

    private GuestEntity persistGuest(String name) {
        GuestEntity guest = new GuestEntity();
        guest.setName(name);
        guest.setEmail(name.toLowerCase().replace(' ', '.') + "-" + UUID.randomUUID() + "@example.com");
        return guestRepository.saveAndFlush(guest);
    }

    private MenuItemEntity persistMenuItem(String name, String category, MenuDepartment department, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name);
        item.setDescription("Menu item used only by ReportServiceTests");
        item.setCategory(category);
        item.setDepartment(department);
        item.setPrice(new BigDecimal(price));
        return menuItemRepository.saveAndFlush(item);
    }

    private record Line(MenuItemEntity item, int quantity, String unitPrice) {
    }

    private static Line line(MenuItemEntity item, int quantity, String unitPrice) {
        return new Line(item, quantity, unitPrice);
    }

    private OrderEntity persistOrder(OrderStatus status, Line... lines) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(user.getId());
        order.setStatus(status);
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : lines) total = total.add(new BigDecimal(l.unitPrice()).multiply(BigDecimal.valueOf(l.quantity())));
        order.setTotal(total);
        order = orderRepository.saveAndFlush(order);
        for (Line l : lines) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setMenuItemId(l.item().getId());
            item.setQuantity(l.quantity());
            item.setUnitPrice(new BigDecimal(l.unitPrice()));
            orderItemRepository.saveAndFlush(item);
        }
        return order;
    }

    private void persistPayment(OrderEntity order, PaymentMethod method, LocalDateTime createdAtUtc) {
        PaymentEntity payment = newPayment(order, method, order.getTotal().toPlainString());
        payment = paymentRepository.saveAndFlush(payment);
        entityManager.createNativeQuery("UPDATE \"Payment\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, createdAtUtc)
                .setParameter(2, payment.getId())
                .executeUpdate();
        entityManager.clear();
    }

    private void persistPayment(OrderEntity order, PaymentMethod method, BookingEntity chargedTo, String amount) {
        PaymentEntity payment = newPayment(order, method, amount);
        payment.setBookingId(chargedTo.getId());
        paymentRepository.saveAndFlush(payment);
    }

    private PaymentEntity newPayment(OrderEntity order, PaymentMethod method, String amount) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setAmount(new BigDecimal(amount));
        payment.setRecordedByUserId(user.getId());
        payment.setShiftId(shift.getId());
        return payment;
    }

    private OccupancyReportRow rowFor(OccupancyReport report) {
        return report.getRooms().stream()
                .filter(r -> room.getId().equals(r.getRoomId().get()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row for the test room"));
    }

    private static List<OccupancyReportRow> concat(List<OccupancyReportRow> rows, OccupancyReportRow total) {
        List<OccupancyReportRow> all = new java.util.ArrayList<>(rows);
        all.add(total);
        return all;
    }

    private static BigDecimal sum(List<String> amounts) {
        return amounts.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
