package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.OverstayChargeEntity;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.JournalSourceType;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RevenueCode;
import com.sunsetbeach.model.RevenueStatisticRow;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.JournalEntryRepository;
import com.sunsetbeach.repository.OverstayChargeRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
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
 * A guest still {@code CHECKED_IN} after their check-out is charged for the nights: a row per night, a ledger
 * entry per night, a line of the folio. "Today" is pinned to 2033-03-20 (Bangkok). The booking is 15-18 March
 * (nights 15, 16, 17, 1000 each); the guest stays on, so the 18th, 19th and tonight, the 20th, are overstay
 * nights. The room's price grid has a manual 2500 for the 19th. Report figures are deltas - the reports and the
 * ledger are property-wide.
 */
@SpringBootTest
@Transactional
class OverstayChargeTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = LocalDateTime.parse("2033-03-20T10:00").atZone(BANGKOK).toInstant();
    private static final String FROM = "2033-03-18";
    private static final String TO = "2033-03-20";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, BANGKOK);
        }
    }

    @Autowired private OverstayChargeService chargeService;
    @Autowired private BookingWriter bookingWriter;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private ReportService reportService;
    @Autowired private LedgerService ledgerService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RatePlanRepository ratePlanRepository;
    @Autowired private OverstayChargeRepository chargeRepository;
    @Autowired private JournalEntryRepository entryRepository;
    @Autowired private com.sunsetbeach.repository.GuestRepository guestRepository;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private RoomUnitEntity unit;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Overstay Charge Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by OverstayChargeTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("OC-" + UUID.randomUUID().toString().substring(0, 8));
        unit = roomUnitRepository.saveAndFlush(newUnit);

        RatePlanEntity peak = new RatePlanEntity();
        peak.setRoomId(room.getId());
        peak.setDate(LocalDate.parse("2033-03-19"));
        peak.setPrice(new BigDecimal("2500.00"));
        ratePlanRepository.saveAndFlush(peak);
    }

    @Test
    void threeNightsOverTheBooking_areInTheFolio_andZ120EqualsZ410EqualsTheManagerReport() {
        BigDecimal z120Before = roomRevenueNetInLedger();
        BigDecimal managerBefore = managerRoomRevenue();
        RevenueStatisticRow z410Before = roomRow();

        BookingEntity booking = checkedInStay("2033-03-15", "2033-03-18");
        chargeService.reconcile(booking.getId());

        // Three nights, priced like any night: the manual price on the 19th, the base price on the others.
        List<OverstayChargeEntity> charges = activeCharges(booking);
        assertThat(charges).extracting(c -> c.getDate().toString()).containsExactly("2033-03-18", "2033-03-19", "2033-03-20");
        assertThat(charges).extracting(c -> c.getPrice().toPlainString()).containsExactly("1000.00", "2500.00", "1000.00");

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(folio.getOverstayNights()).isEqualTo(3);
        assertThat(folio.getOverstayTotal()).isEqualTo("4500.00");
        assertThat(folio.getFolioTotal()).isEqualTo("7500.00"); // 3 agreed nights + 3 overstay nights
        assertThat(folio.getBalanceDue()).isEqualTo("7500.00");
        // The debt has fallen due: the guest is in the house.
        assertThat(BookingFolioCalculator.debtHasFallenDue(
                        bookingRepository.findById(booking.getId()).orElseThrow(), new BigDecimal(folio.getBalanceDue()), LocalDate.parse(TO)))
                .isTrue();

        // Same room revenue everywhere, for a period that holds the overstay nights.
        BigDecimal z120 = roomRevenueNetInLedger().subtract(z120Before);
        RevenueStatisticRow z410 = roomRow();
        BigDecimal z410Net = new BigDecimal(z410.getNet()).subtract(new BigDecimal(z410Before.getNet()));
        BigDecimal z410Gross = new BigDecimal(z410.getGross()).subtract(new BigDecimal(z410Before.getGross()));
        BigDecimal manager = managerRoomRevenue().subtract(managerBefore);
        assertThat(z410Gross).isEqualByComparingTo("4500.00");
        assertThat(z120).isEqualByComparingTo(z410Net);
        assertThat(manager).isEqualByComparingTo(z410Gross);
    }

    @Test
    void recalculating_createsNoDuplicates_inRowsOrLedger() {
        BookingEntity booking = checkedInStay("2033-03-15", "2033-03-18");

        chargeService.reconcile(booking.getId());
        chargeService.reconcile(booking.getId());
        chargeService.sweep();
        bookingService.getFolio(booking.getId());

        assertThat(chargeRepository.findByBookingIdOrderByDateAsc(booking.getId())).hasSize(3);
        for (OverstayChargeEntity charge : chargeRepository.findByBookingIdOrderByDateAsc(booking.getId())) {
            assertThat(entryRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType.OVERSTAY, charge.getId())).hasSize(1);
        }
        assertThat(bookingService.getFolio(booking.getId()).getOverstayTotal()).isEqualTo("4500.00");
    }

    @Test
    void extendingTheStayOverTheNights_makesThemOrdinary_withoutCountingThemTwice() {
        RevenueStatisticRow z410Baseline = roomRow();
        BigDecimal managerBaseline = managerRoomRevenue();
        BookingEntity booking = checkedInStay("2033-03-15", "2033-03-18");
        chargeService.reconcile(booking.getId());
        assertThat(activeCharges(booking)).hasSize(3);

        // Staff extend the booking to the 21st: the 18th, 19th and 20th are now nights of the booking.
        bookingService.updateSchedule(booking.getId(), new BookingScheduleInput("2033-03-15", "2033-03-21").roomUnitId(unit.getId()));
        entityManager.flush();
        entityManager.clear();

        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(folio.getOverstayNights()).isZero();
        assertThat(folio.getOverstayTotal()).isEqualTo("0.00");
        // The extension priced the three new nights by the grid: 1000 + 2500 + 1000, on top of the 3000 already agreed.
        assertThat(folio.getRoomTotal()).isEqualTo("7500.00");
        assertThat(folio.getFolioTotal()).isEqualTo("7500.00");
        // The voided nights were reversed in the ledger (never edited or deleted) and kept as rows.
        List<OverstayChargeEntity> all = chargeRepository.findByBookingIdOrderByDateAsc(booking.getId());
        assertThat(all).hasSize(3).allSatisfy(c -> {
            assertThat(c.getVoidedAt()).isNotNull();
            assertThat(c.getVoidEntryId()).isNotNull();
        });

        // The three nights are counted once, as nights of the booking (the reports spread a segment's price evenly:
        // 7500 over 6 nights, 1250 each) - not as booking nights plus overstay nights.
        RevenueStatisticRow z410 = roomRow();
        assertThat(new BigDecimal(z410.getGross()).subtract(new BigDecimal(z410Baseline.getGross()))).isEqualByComparingTo("3750.00");
        assertThat(managerRoomRevenue().subtract(managerBaseline)).isEqualByComparingTo("3750.00");

        // Running it again changes nothing.
        chargeService.reconcile(booking.getId());
        assertThat(activeCharges(booking)).isEmpty();
    }

    @Test
    void checkingOut_keepsTheNightsSlept_andReleasesTonight() {
        BookingEntity booking = checkedInStay("2033-03-15", "2033-03-18");
        chargeService.reconcile(booking.getId());

        occupancyService.checkOut(booking.getId(), null);
        entityManager.flush();
        entityManager.clear();

        // Slept: the 18th and the 19th. The 20th is today - they leave before it.
        BookingFolio folio = bookingService.getFolio(booking.getId());
        assertThat(folio.getOverstayNights()).isEqualTo(2);
        assertThat(folio.getOverstayTotal()).isEqualTo("3500.00");
        assertThat(folio.getBalanceDue()).isEqualTo("6500.00");
        // Running it again changes nothing.
        chargeService.reconcile(booking.getId());
        assertThat(bookingService.getFolio(booking.getId()).getOverstayTotal()).isEqualTo("3500.00");
    }

    @Test
    void guestLtv_ranksOnTheOverstayNightsToo() {
        BookingEntity booking = checkedInStay("2033-03-15", "2033-03-18");
        com.sunsetbeach.entity.GuestEntity guest = new com.sunsetbeach.entity.GuestEntity();
        guest.setName("LTV Overstay Guest " + UUID.randomUUID());
        guest = guestRepository.saveAndFlush(guest);
        String guestId = guest.getId();
        BookingEntity linked = bookingRepository.findById(booking.getId()).orElseThrow();
        linked.setGuestId(guestId);
        bookingRepository.saveAndFlush(linked);
        entityManager.clear();
        chargeService.reconcile(booking.getId());

        var totals = bookingRepository.sumByGuest(com.sunsetbeach.model.BookingStatus.CANCELLED, org.springframework.data.domain.PageRequest.of(0, 5000)).stream()
                .filter(t -> guestId.equals(t.getGuestId()))
                .findFirst()
                .orElseThrow();
        // 3 agreed nights (3000) + 3 overstay nights (4500).
        assertThat(totals.getRoomRevenue()).isEqualByComparingTo("7500.00");
    }

    @Test
    void aGuestWhoHasNotArrived_orHasAlreadyLeft_getsNoOverstay() {
        BookingEntity expected = stay("2033-03-10", "2033-03-12", OccupancyStatus.EXPECTED);
        BookingEntity gone = stay("2033-03-13", "2033-03-15", OccupancyStatus.CHECKED_OUT);

        chargeService.reconcile(expected.getId());
        chargeService.reconcile(gone.getId());

        assertThat(chargeRepository.findByBookingIdOrderByDateAsc(expected.getId())).isEmpty();
        assertThat(chargeRepository.findByBookingIdOrderByDateAsc(gone.getId())).isEmpty();
        assertThat(bookingService.getFolio(expected.getId()).getOverstayNights()).isZero();
    }

    // --- helpers ---------------------------------------------------------------------------

    private BookingEntity checkedInStay(String checkIn, String checkOut) {
        return stay(checkIn, checkOut, OccupancyStatus.CHECKED_IN);
    }

    private BookingEntity stay(String checkIn, String checkOut, OccupancyStatus occupancy) {
        BookingEntity booking = bookingWriter.insertStaff(room, "Overstay Guest", "overstay@example.com", "+66800000000", LocalDate.parse(checkIn),
                LocalDate.parse(checkOut), BookingChannel.DIRECT, BookingPurpose.STANDARD, 2, 0, unit.getId(), null);
        booking.setOccupancyStatus(occupancy);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();
        return saved;
    }

    private List<OverstayChargeEntity> activeCharges(BookingEntity booking) {
        return chargeRepository.findByBookingIdAndVoidedAtIsNull(booking.getId()).stream()
                .sorted(Comparator.comparing(OverstayChargeEntity::getDate))
                .toList();
    }

    private RevenueStatisticRow roomRow() {
        return reportService.revenueStatistic(FROM, TO).getRows().stream().filter(r -> r.getCode() == RevenueCode.ROOM).findFirst().orElseThrow();
    }

    /** Sum of room revenue over the three days, from the manager report (one day at a time). */
    private BigDecimal managerRoomRevenue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (String day : List.of("2033-03-18", "2033-03-19", "2033-03-20")) {
            sum = sum.add(new BigDecimal(reportService.manager(day).getToday().getRevenue().getRoomRevenue()));
        }
        return sum;
    }

    /** Z120's Room Revenue account (net of VAT). */
    private BigDecimal roomRevenueNetInLedger() {
        return ledgerService.trialBalance(TO).getRows().stream()
                .filter(r -> "4000".equals(r.getAccountCode()))
                .map(r -> new BigDecimal(r.getBalance()))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
