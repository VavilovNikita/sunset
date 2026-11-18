package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.BookingTrackerSnapshotEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.BookingTrackerChannelRow;
import com.sunsetbeach.model.BookingTrackerMonth;
import com.sunsetbeach.model.BookingTrackerMonthTargetsInput;
import com.sunsetbeach.model.BookingTrackerReport;
import com.sunsetbeach.model.BookingTrackerSnapshot;
import com.sunsetbeach.model.BookingTrackerSnapshotUpdateInput;
import com.sunsetbeach.model.BreakfastRate;
import com.sunsetbeach.model.MarketSegment;
import com.sunsetbeach.model.MarketSegmentRow;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ServiceChargeSettingsUpdateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.BookingTrackerMonthRepository;
import com.sunsetbeach.repository.BookingTrackerSnapshotRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.support.FixedHotelClockConfig;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The booking tracker against hand-built fixtures in 2037 (stays) and the shared fixed hotel clock (2034-03-10, a
 * Friday, 12:20 Bangkok). Every figure is computed by hand in the test, not by calling the code under test again.
 * The expected arithmetic is the workbook's: package = gross / 1.10 / 1.07 (service charge 10%, VAT 7%), service charge
 * = package x 10%, VAT = gross - package - service charge, breakfast = nights x guests x rate, net = package - breakfast,
 * each step rounded to two decimals.
 */
@SpringBootTest
@Transactional
@Import(FixedHotelClockConfig.class)
class BookingTrackerTests extends AbstractIntegrationTest {

    @Autowired private BookingTrackerService tracker;
    @Autowired private BookingTrackerSnapshotService snapshots;
    @Autowired private BookingTrackerExportService exporter;
    @Autowired private ServiceChargeSettingsService serviceChargeSettings;
    @Autowired private ReportService reportService;
    @Autowired private BookingService bookingService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private RoomUnitBlockRepository blockRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingSegmentRepository segmentRepository;
    @Autowired private BookingSegmentNightlyRateRepository nightlyRateRepository;
    @Autowired private BookingTrackerMonthRepository monthRepository;
    @Autowired private BookingTrackerSnapshotRepository snapshotRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    private RoomEntity room;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        room = persistRoom();
        UserEntity u = new UserEntity();
        u.setEmail("tracker-" + UUID.randomUUID() + "@example.com");
        u.setName(u.getEmail());
        u.setPasswordHash("irrelevant-for-this-test");
        u.setRole(Role.MANAGER);
        user = userRepository.saveAndFlush(u);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(user.getId(), user.getEmail(), Role.MANAGER), null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    /** Audit rows commit on their own (REQUIRES_NEW) and survive this class's rollback. */
    @AfterEach
    void cleanAudit() {
        SecurityContextHolder.clearContext();
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_TRACKER_TARGET_UPDATED
                        || e.getAction() == AuditAction.BOOKING_TRACKER_SNAPSHOT_UPDATED
                        || e.getAction() == AuditAction.SERVICE_CHARGE_SETTINGS_UPDATED
                        || (e.getAction() == AuditAction.BOOKING_STATUS_CHANGED && e.getActorUserId().equals(user.getId())))
                .toList());
    }

    // --- The workbook's own rows ---------------------------------------------------------------

    @Test
    void workbookExampleRows_matchTheWorkbookFormulas_stepByStepRounded() {
        BookingEntity expedia = stay("2037-11-21", "2037-11-26", BookingChannel.EXPEDIA, BookingStatus.CONFIRMED, 5, "6577.19");
        breakfast(expedia, 2, 250);
        BookingEntity agoda = stay("2037-11-25", "2037-11-29", BookingChannel.AGODA, BookingStatus.CONFIRMED, 4, "4342.48");
        breakfast(agoda, 2, 250);

        BookingTrackerService.Computation c = tracker.computation("2037-11", "2037-11", null);
        assertThat(c.report().getVatRate()).isEqualTo("7.00");
        assertThat(c.report().getServiceChargeRate()).isEqualTo("10.00");

        // Expedia, Beach Front Private Pool, 21-26 Nov, 32,885.95, 2 guests at 250: 5 nights.
        BookingTrackerService.Figures e = row(c, expedia).months().get(YearMonth.of(2037, 11));
        assertFigures(e, 5, "32885.95", "27940.48", "2794.05", "2151.42", "2500.00", "25440.48");
        // Agoda, Sunset Jacuzzi Sea View, 25-29 Nov, 17,369.92, 2 guests at 250: 4 nights.
        BookingTrackerService.Figures a = row(c, agoda).months().get(YearMonth.of(2037, 11));
        assertFigures(a, 4, "17369.92", "14757.79", "1475.78", "1136.35", "2000.00", "12757.79");

        // The month adds the rounded rows - never re-rounds a sum.
        BookingTrackerMonth month = c.report().getMonths().get(0);
        assertThat(month.getBookedRoomNights()).isEqualTo(9);
        assertThat(month.getGrossRevenue()).isEqualTo("50255.87");
        assertThat(month.getPackageRevenue()).isEqualTo("42698.27");
        assertThat(month.getServiceCharge()).isEqualTo("4269.83");
        assertThat(month.getVat()).isEqualTo("3287.77");
        assertThat(month.getBreakfastRevenue()).isEqualTo("4500.00");
        assertThat(month.getNetRoomRevenue()).isEqualTo("38198.27");
        assertThat(month.getNetAdr().get()).isEqualTo("4244.25"); // 38198.27 / 9
        // The three parts of the gross add back to it, to the cent.
        assertThat(new BigDecimal(month.getPackageRevenue()).add(new BigDecimal(month.getServiceCharge())).add(new BigDecimal(month.getVat())))
                .isEqualByComparingTo(month.getGrossRevenue());
    }

    @Test
    void noBreakfastIsDeducted_whenTheFieldsAreEmpty_orGuestsAreZero() {
        BookingEntity none = stay("2037-11-02", "2037-11-04", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        BookingEntity zero = stay("2037-11-06", "2037-11-08", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        zero.setBreakfastGuests(0);
        zero.setBreakfastRate(350);
        bookingRepository.saveAndFlush(zero);

        BookingTrackerService.Computation c = tracker.computation("2037-11", "2037-11", null);
        assertThat(row(c, none).months().get(YearMonth.of(2037, 11)).breakfast()).isEqualByComparingTo("0");
        assertThat(row(c, zero).months().get(YearMonth.of(2037, 11)).breakfast()).isEqualByComparingTo("0");
        assertThat(c.report().getMonths().get(0).getNetRoomRevenue()).isEqualTo(c.report().getMonths().get(0).getPackageRevenue());
    }

    @Test
    void aStayAcrossMonths_isSplitByItsNights_eachAtItsOwnFrozenPrice() {
        // 29 Nov - 3 Dec, four nights at different frozen prices; breakfast for one guest at 350.
        BookingEntity b = stay("2037-11-29", "2037-12-03", BookingChannel.DIRECT, BookingStatus.PAID, 0, "1000.00", "1100.00", "2000.00", "2200.00");
        breakfast(b, 1, 350);

        BookingTrackerService.Computation c = tracker.computation("2037-11", "2037-12", null);
        BookingTrackerService.BookingRow r = row(c, b);
        assertFigures(r.months().get(YearMonth.of(2037, 11)), 2, "2100.00", "1784.20", "178.42", "137.38", "700.00", "1084.20");
        assertFigures(r.months().get(YearMonth.of(2037, 12)), 2, "4200.00", "3568.39", "356.84", "274.77", "700.00", "2868.39");

        List<BookingTrackerMonth> months = c.report().getMonths();
        assertThat(months).extracting(m -> m.getMonth().get()).containsExactly("2037-11", "2037-12");
        assertThat(months.get(0).getBookedRoomNights()).isEqualTo(2);
        assertThat(months.get(1).getBookedRoomNights()).isEqualTo(2);
        assertThat(months.get(0).getGrossRevenue()).isEqualTo("2100.00");
        assertThat(months.get(1).getGrossRevenue()).isEqualTo("4200.00");
        BookingTrackerMonth total = c.report().getTotal();
        assertThat(total.getMonth().get()).isNull();
        assertThat(total.getBookedRoomNights()).isEqualTo(4);
        assertThat(total.getNetRoomRevenue()).isEqualTo("3952.59"); // 1084.20 + 2868.39
    }

    @Test
    void anOverstay_countsItsNightsAtTheChargedPrice() {
        BookingEntity b = stay("2037-11-10", "2037-11-12", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        b.setOccupancyStatus(OccupancyStatus.CHECKED_OUT);
        bookingRepository.saveAndFlush(b);
        jdbcTemplate.update("INSERT INTO \"OverstayCharge\" (\"id\", \"bookingId\", \"date\", \"price\") VALUES (?, ?, DATE '2037-11-12', 1500.00)",
                UUID.randomUUID().toString(), b.getId());
        entityManager.clear();

        BookingTrackerService.BookingRow r = row(tracker.computation("2037-11", "2037-11", null), b);
        BookingTrackerService.Figures f = r.months().get(YearMonth.of(2037, 11));
        assertThat(f.nights()).isEqualTo(3);
        assertThat(f.gross()).isEqualByComparingTo("3500.00");
    }

    // --- The population ------------------------------------------------------------------------

    @Test
    void countedBookingsOnly_andAgreesWithTheOccupancyPopulationForTheSameNights() {
        stay("2037-09-10", "2037-09-13", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 3, "1000.00");             // 3 nights, counted
        stay("2037-09-20", "2037-09-22", BookingChannel.AGODA, BookingStatus.PAID, 2, "1000.00");                   // 2, counted
        stay("2037-08-30", "2037-09-03", BookingChannel.PHONE, BookingStatus.CONFIRMED, 4, "1000.00");              // 2 of its 4 nights are in September
        BookingEntity enquiry = stay("2037-09-05", "2037-09-09", BookingChannel.DIRECT, BookingStatus.NEW, 4, "1000.00");   // the workbook's "Enquiry"
        stay("2037-09-12", "2037-09-17", BookingChannel.DIRECT, BookingStatus.CANCELLED, 5, "1000.00");
        BookingEntity noShow = stay("2037-09-25", "2037-09-27", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        noShow.setOccupancyStatus(OccupancyStatus.NO_SHOW);
        bookingRepository.saveAndFlush(noShow);

        BookingTrackerMonth month = tracker.report("2037-09", "2037-09", null).getMonths().get(0);
        long occupancySold = reportService.occupancy("2037-09-01", "2037-09-30").getTotal().getRoomNightsSold();

        assertThat(month.getBookedRoomNights()).isEqualTo(3 + 2 + 2);
        // Same nights, same population, status filter aside: occupancy counts everything not cancelled.
        long enquiryNights = 4;
        long noShowNights = 2;
        assertThat(occupancySold - enquiryNights - noShowNights).isEqualTo((long) month.getBookedRoomNights());
        assertThat(enquiry.getStatus()).isEqualTo(BookingStatus.NEW);
    }

    @Test
    void availability_isActiveUnitsTimesNights_lessTheNightsUnitsAreBlocked_countedOncePerUnitAndNight() {
        RoomUnitEntity u1 = persistUnit(true);
        RoomUnitEntity u2 = persistUnit(true);
        RoomUnitEntity inactive = persistUnit(false);
        block(u1, "2037-10-05", "2037-10-08");
        block(u1, "2037-10-07", "2037-10-10"); // overlaps the first: 5-10 is six nights, not seven
        block(u1, "2037-10-30", "2037-11-02"); // 30, 31 Oct and 1, 2 Nov
        block(u2, "2037-09-28", "2037-10-02"); // Oct 1 and 2 only
        block(inactive, "2037-10-01", "2037-10-31"); // an inactive unit is not in the denominator, so its block is ignored

        long activeUnits = roomUnitRepository.findAll().stream().filter(RoomUnitEntity::isActive).count();
        BookingTrackerReport report = tracker.report("2037-10", "2037-11", null);
        BookingTrackerMonth oct = report.getMonths().get(0);
        BookingTrackerMonth nov = report.getMonths().get(1);

        assertThat(oct.getPhysicalRooms().get()).isEqualTo((int) activeUnits);
        assertThat(oct.getNightsInMonth()).isEqualTo(31);
        assertThat(oct.getBlockedRoomNights()).isEqualTo(6 + 2 + 2); // u1: 5-10 and 30-31; u2: 1-2
        assertThat(oct.getAvailableRoomNights()).isEqualTo((int) (activeUnits * 31 - 10));
        assertThat(nov.getBlockedRoomNights()).isEqualTo(2);
        assertThat(nov.getAvailableRoomNights()).isEqualTo((int) (activeUnits * 30 - 2));
        assertThat(report.getTotal().getAvailableRoomNights()).isEqualTo(oct.getAvailableRoomNights() + nov.getAvailableRoomNights());
        assertThat(report.getTotal().getPhysicalRooms().get()).isNull();
    }

    @Test
    void occupancy_isBookedOverAvailable_andNullWhenNothingIsAvailable() {
        stay("2037-07-01", "2037-07-04", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 3, "1000.00");
        RoomUnitEntity unit = persistUnit(true);
        BookingTrackerMonth month = tracker.report("2037-07", "2037-07", null).getMonths().get(0);
        long units = roomUnitRepository.findAll().stream().filter(RoomUnitEntity::isActive).count();
        assertThat(month.getAvailableRoomNights()).isEqualTo((int) (units * 31));
        BigDecimal expected = BigDecimal.valueOf(3 * 100.0 / (units * 31)).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(month.getOccupancyPercent().get()).isEqualTo(expected.toPlainString());

        // Every room blocked for the whole month: nothing is available, so there is no occupancy to show (never a division by zero).
        roomUnitRepository.findAll().stream().filter(RoomUnitEntity::isActive).forEach(u -> block(u, "2037-07-01", "2037-07-31"));
        BookingTrackerMonth blocked = tracker.report("2037-07", "2037-07", null).getMonths().get(0);
        assertThat(blocked.getAvailableRoomNights()).isZero();
        assertThat(blocked.getOccupancyPercent().get()).isNull();
        assertThat(unit.isActive()).isTrue();
    }

    // --- Targets and baseline ------------------------------------------------------------------

    @Test
    void targetsAndBaseline_areStoredPerMonth_andDriveStillNeededAndPickup() {
        stay("2037-12-10", "2037-12-14", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 4, "1000.00");

        BookingTrackerMonthTargetsInput input = new BookingTrackerMonthTargetsInput().targetRoomNights(10).baselineRoomNights(3).baselineNetRevenue("1234.50");
        tracker.updateMonth("2037-12", input);

        BookingTrackerMonth month = tracker.report("2037-12", "2037-12", null).getMonths().get(0);
        assertThat(month.getTargetRoomNights().get()).isEqualTo(10);
        assertThat(month.getStillNeededRoomNights().get()).isEqualTo(6);   // 10 - 4
        assertThat(month.getBaselineRoomNights().get()).isEqualTo(3);
        assertThat(month.getPickupVsBaseline().get()).isEqualTo(1);        // 4 - 3
        assertThat(month.getBaselineNetRevenue().get()).isEqualTo("1234.50");
        BookingTrackerMonth total = tracker.report("2037-12", "2037-12", null).getTotal();
        assertThat(total.getTargetRoomNights().get()).isEqualTo(10);
        assertThat(total.getStillNeededRoomNights().get()).isEqualTo(6);

        // Already above target: nothing still needed, never negative.
        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput().targetRoomNights(2).baselineRoomNights(3).baselineNetRevenue("1234.50"));
        assertThat(tracker.report("2037-12", "2037-12", null).getMonths().get(0).getStillNeededRoomNights().get()).isZero();

        // No target typed: nothing to subtract from.
        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput().baselineRoomNights(3));
        BookingTrackerMonth noTarget = tracker.report("2037-12", "2037-12", null).getMonths().get(0);
        assertThat(noTarget.getTargetRoomNights().get()).isNull();
        assertThat(noTarget.getStillNeededRoomNights().get()).isNull();
        assertThat(noTarget.getBaselineNetRevenue().get()).isNull();
    }

    @Test
    void anUnchangedSave_writesNothing_notEvenAnAuditEntry_andARealChangeIsAudited() {
        BookingTrackerMonthTargetsInput input = new BookingTrackerMonthTargetsInput().targetRoomNights(10).baselineRoomNights(3).baselineNetRevenue("1234.50");
        tracker.updateMonth("2037-12", input);
        assertThat(targetAudits("2037-12")).hasSize(1);
        var updatedAt = monthRepository.findById("2037-12").orElseThrow().getUpdatedAt();

        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput().targetRoomNights(10).baselineRoomNights(3).baselineNetRevenue("1234.5"));
        assertThat(targetAudits("2037-12")).hasSize(1);
        assertThat(monthRepository.findById("2037-12").orElseThrow().getUpdatedAt()).isEqualTo(updatedAt);

        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput().targetRoomNights(11).baselineRoomNights(3).baselineNetRevenue("1234.50"));
        assertThat(targetAudits("2037-12")).hasSize(2);
        assertThat(targetAudits("2037-12").get(1).getSummary()).contains("2037-12").contains("10 -> 11");

        // Clearing every field leaves no row; saving nothing over nothing writes nothing.
        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput());
        assertThat(monthRepository.findById("2037-12")).isEmpty();
        assertThat(targetAudits("2037-12")).hasSize(3);
        tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput());
        assertThat(targetAudits("2037-12")).hasSize(3);
    }

    @Test
    void baselineRevenue_withMoreThanTwoDecimals_orAMalformedMonth_isRefused() {
        assertThatThrownBy(() -> tracker.updateMonth("2037-12", new BookingTrackerMonthTargetsInput().baselineNetRevenue("1.234")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> tracker.updateMonth("2037-13", new BookingTrackerMonthTargetsInput().targetRoomNights(1)))
                .isInstanceOf(ValidationException.class);
    }

    // --- Channel summary and booking trends ----------------------------------------------------

    @Test
    void channelSummary_hasSeparateRowsPerChannel_sharesAndARankingByNetRevenue_andTheStayMonthFilter() {
        stay("2037-07-10", "2037-07-12", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");   // gross 2000
        stay("2037-07-20", "2037-07-23", BookingChannel.EXPEDIA, BookingStatus.PAID, 3, "2000.00");       // gross 6000
        stay("2037-07-02", "2037-07-03", BookingChannel.CTRIP, BookingStatus.CONFIRMED, 1, "3000.00");    // gross 3000
        stay("2037-07-05", "2037-07-06", BookingChannel.PHONE, BookingStatus.CONFIRMED, 1, "1000.00");
        stay("2037-07-07", "2037-07-08", BookingChannel.AIRBNB, BookingStatus.CONFIRMED, 1, "1000.00");
        stay("2037-08-01", "2037-08-03", BookingChannel.AGODA, BookingStatus.CONFIRMED, 2, "1000.00");

        BookingTrackerReport july = tracker.report("2037-07", "2037-08", "2037-07");
        assertThat(july.getStayMonth().get()).isEqualTo("2037-07");
        assertThat(channel(july, BookingChannel.DIRECT).getRoomNights()).isEqualTo(2);
        assertThat(channel(july, BookingChannel.PHONE).getRoomNights()).isEqualTo(1);   // PHONE and AIRBNB are their own rows
        assertThat(channel(july, BookingChannel.AIRBNB).getRoomNights()).isEqualTo(1);
        assertThat(channel(july, BookingChannel.EXPEDIA).getRoomNights()).isEqualTo(3);
        assertThat(channel(july, BookingChannel.CTRIP).getRoomNights()).isEqualTo(1);
        assertThat(channel(july, BookingChannel.AGODA).getRoomNights()).isZero();       // August is outside the filter
        assertThat(july.getChannels().getTotal().getRoomNights()).isEqualTo(8);
        assertThat(july.getChannels().getTotal().getChannel().get()).isNull();
        assertThat(july.getChannels().getRows()).hasSize(BookingChannel.values().length);
        // Shares are of the filtered total.
        assertThat(channel(july, BookingChannel.EXPEDIA).getRoomNightSharePercent().get()).isEqualTo("37.50");
        assertThat(channel(july, BookingChannel.EXPEDIA).getNetRoomRevenue()).isEqualTo("5097.71");
        assertThat(channel(july, BookingChannel.EXPEDIA).getNetAdr().get()).isEqualTo("1699.24");
        // Ranked by net revenue: Expedia 5097.71, Ctrip 2548.85, Direct 1699.24, then the 849.62 pair (more nights first - none, so enum order).
        assertThat(july.getChannels().getRanking()).extracting(r -> r.getChannel()).containsExactly(
                BookingChannel.EXPEDIA, BookingChannel.CTRIP, BookingChannel.DIRECT, BookingChannel.PHONE, BookingChannel.AIRBNB);
        assertThat(july.getChannels().getRanking()).extracting(r -> r.getRank()).containsExactly(1, 2, 3, 4, 5);

        BookingTrackerReport all = tracker.report("2037-07", "2037-08", null);
        assertThat(all.getStayMonth().get()).isNull();
        assertThat(channel(all, BookingChannel.AGODA).getRoomNights()).isEqualTo(2);
        assertThat(all.getChannels().getTotal().getRoomNights()).isEqualTo(10);
        // The months section is never narrowed by the filter.
        assertThat(july.getMonths()).hasSize(2);
    }

    @Test
    void bookingWindow_usesTheHotelDayOfCreation_orSiteMindersBookedOn_andBucketsAndAveragesByRoomNights() {
        // Created 30 Jun 20:00 UTC = 1 Jul 03:00 in Bangkok: the booking day is 1 Jul, so a 10 Jul arrival has a 9-day window.
        BookingEntity direct = stay("2037-07-10", "2037-07-12", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        createdAtUtc(direct, "2037-06-30T20:00:00");
        // 20 Jul arrival, made on 1 Mar Bangkok (00:00 UTC is 07:00 there): 141 days.
        BookingEntity early = stay("2037-07-20", "2037-07-23", BookingChannel.EXPEDIA, BookingStatus.CONFIRMED, 3, "2000.00");
        createdAtUtc(early, "2037-03-01T00:00:00");
        // A SiteMinder import row created on 15 Jun, but SiteMinder says it was booked on 29 Jun: window 3, not 17.
        BookingEntity siteMinder = stay("2037-07-02", "2037-07-03", BookingChannel.CTRIP, BookingStatus.CONFIRMED, 1, "3000.00");
        siteMinder.setSource(BookingSource.SITEMINDER);
        siteMinder.setExternalReference("TRK-" + UUID.randomUUID());
        siteMinder.setBookedAt(LocalDateTime.parse("2037-06-29T01:00:00"));
        bookingRepository.saveAndFlush(siteMinder);
        createdAtUtc(siteMinder, "2037-06-15T00:00:00");

        BookingTrackerService.Computation c = tracker.computation("2037-07", "2037-07", null);
        assertThat(row(c, direct).windowDays()).isEqualTo(9);
        assertThat(row(c, direct).bookingDate()).isEqualTo(LocalDate.of(2037, 7, 1));
        assertThat(row(c, early).windowDays()).isEqualTo(141);
        assertThat(row(c, siteMinder).windowDays()).isEqualTo(3);
        assertThat(row(c, siteMinder).bookingDate()).isEqualTo(LocalDate.of(2037, 6, 29));

        var trends = c.report().getTrends();
        assertThat(trends.getBuckets()).extracting(b -> b.getLabel()).containsExactly("0-3", "4-7", "8-14", "15-30", "31-60", "61-90", "91+");
        assertThat(trends.getBuckets()).extracting(b -> b.getRoomNights()).containsExactly(1, 0, 2, 0, 0, 0, 3);
        assertThat(trends.getBuckets().get(0).getNetRoomRevenue()).isEqualTo("2548.85");
        assertThat(trends.getBuckets().get(6).getRoomNightSharePercent().get()).isEqualTo("50.00");
        assertThat(trends.getBuckets().get(6).getToDays().get()).isNull();
        // (9 x 2 + 141 x 3 + 3 x 1) / 6 room nights.
        assertThat(trends.getAverageBookingWindowDays().get()).isEqualTo("74.00");
    }

    @Test
    void aWindowBelowZero_isCountedAsZero() {
        BookingEntity backEntry = stay("2037-07-10", "2037-07-11", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 1, "1000.00");
        createdAtUtc(backEntry, "2037-07-20T00:00:00");
        BookingTrackerService.Computation c = tracker.computation("2037-07", "2037-07", null);
        assertThat(row(c, backEntry).windowDays()).isZero();
        assertThat(c.report().getTrends().getBuckets().get(0).getRoomNights()).isEqualTo(1);
    }

    @Test
    void bookingPace_isTheLastTwelveMondayWeeks_ofBookingsMadeInThem_forTheNightsInsideTheFilter() {
        // Today is Friday 2034-03-10, so the last week starts Monday 2034-03-06 and the first one 2033-12-19.
        BookingEntity thisWeek = stay("2034-05-10", "2034-05-12", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        createdAtUtc(thisWeek, "2034-03-08T03:00:00");
        BookingEntity early = stay("2034-05-20", "2034-05-21", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 1, "1000.00");
        createdAtUtc(early, "2034-01-02T03:00:00"); // a Monday: the third week
        BookingEntity tooOld = stay("2034-05-25", "2034-05-26", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 1, "1000.00");
        createdAtUtc(tooOld, "2033-12-18T03:00:00"); // the Sunday before the first week

        var weeks = tracker.report("2034-05", "2034-05", null).getTrends().getWeeks();
        assertThat(weeks).hasSize(12);
        assertThat(weeks.get(0).getWeekStart()).isEqualTo(LocalDate.of(2033, 12, 19));
        assertThat(weeks.get(11).getWeekStart()).isEqualTo(LocalDate.of(2034, 3, 6));
        assertThat(weeks.get(11).getRoomNights()).isEqualTo(2);
        assertThat(weeks.get(11).getNetRoomRevenue()).isEqualTo("1699.24");
        assertThat(weeks.get(2).getWeekStart()).isEqualTo(LocalDate.of(2034, 1, 2));
        assertThat(weeks.get(2).getRoomNights()).isEqualTo(1);
        assertThat(weeks.get(2).getNetRoomRevenue()).isEqualTo("849.62");
        assertThat(weeks.stream().mapToInt(w -> w.getRoomNights()).sum()).isEqualTo(3); // the 18 December booking is in no week
    }

    // --- Weekly snapshots ----------------------------------------------------------------------

    @Test
    void snapshots_areWrittenOnce_frozen_andAMissedFridayIsWrittenLateAsAtTheMomentItRuns() {
        stay("2033-12-10", "2033-12-14", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 4, "1000.00");
        int decemberNow = bookedNights("2033-12");

        // The shared clock is Friday 2034-03-10 12:20: that Friday's 23:55 has not come, so the Friday owed is 2034-03-03.
        // Two seasons of six months: Nov 2033 - Apr 2034 and Nov 2034 - Apr 2035.
        assertThat(snapshots.runOnce()).isEqualTo(12);
        BookingTrackerSnapshotEntity december = snapshot("2034-03-03", "2033-12");
        assertThat(december.isLate()).isTrue();
        assertThat(december.getRoomNights()).isEqualTo(decemberNow);
        assertThat(snapshotRepository.existsBySnapshotDateAndStayMonth(LocalDate.of(2034, 3, 3), "2035-04")).isTrue();
        assertThat(snapshotRepository.existsBySnapshotDateAndStayMonth(LocalDate.of(2034, 3, 3), "2035-05")).isFalse();
        assertThat(snapshotRepository.existsBySnapshotDateAndStayMonth(LocalDate.of(2034, 3, 10), "2033-12")).isFalse();

        // A repeat run (or a racing one) writes nothing, and a booking made since does not touch what is frozen.
        stay("2033-12-20", "2033-12-23", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 3, "1000.00");
        entityManager.clear();
        assertThat(snapshots.runOnce()).isZero();
        assertThat(snapshot("2034-03-03", "2033-12").getRoomNights()).isEqualTo(decemberNow);
    }

    @Test
    void anOnTimeFriday_isNotLate_andPickupIsTheChangeAgainstThePreviousSnapshotOfThatMonth() {
        stay("2033-12-10", "2033-12-14", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 4, "1000.00");
        int before = bookedNights("2033-12");
        snapshots.runOnce(LocalDateTime.parse("2034-03-03T23:55:10"));
        stay("2033-12-20", "2033-12-23", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 3, "1000.00");
        int after = bookedNights("2033-12");
        assertThat(snapshots.runOnce(LocalDateTime.parse("2034-03-10T23:55:10"))).isEqualTo(12);

        assertThat(snapshot("2034-03-03", "2033-12").isLate()).isFalse();
        assertThat(snapshot("2034-03-10", "2033-12").isLate()).isFalse();

        List<BookingTrackerSnapshot> review = tracker.report("2033-12", "2033-12", null).getWeeklyReview();
        assertThat(review).extracting(s -> s.getSnapshotDate()).containsExactly(LocalDate.of(2034, 3, 10), LocalDate.of(2034, 3, 3));
        assertThat(review.get(0).getRoomNights()).isEqualTo(after);
        assertThat(review.get(0).getPickupRoomNights().get()).isEqualTo(after - before);
        assertThat(review.get(0).getPickupNetRevenue().get()).isNotNull();
        assertThat(review.get(1).getPickupRoomNights().get()).isNull(); // the first one has nothing to compare to
        assertThat(review.get(1).getRoomNights()).isEqualTo(before);
        // The review lists only the months asked for.
        assertThat(tracker.report("2034-01", "2034-01", null).getWeeklyReview()).extracting(s -> s.getStayMonth()).containsOnly("2034-01");
    }

    @Test
    void snapshotNotes_areEditable_trimmed_andAnUnchangedSaveWritesNothing() {
        snapshots.runOnce();
        BookingTrackerSnapshotEntity entity = snapshot("2034-03-03", "2033-12");

        BookingTrackerSnapshot saved = tracker.updateSnapshot(entity.getId(),
                new BookingTrackerSnapshotUpdateInput().reason("  Slow pickup  ").nextAction("Call agents").owner(" Nok ").dueDate(LocalDate.of(2034, 3, 20)));
        assertThat(saved.getReason().get()).isEqualTo("Slow pickup");
        assertThat(saved.getOwner().get()).isEqualTo("Nok");
        assertThat(saved.getDueDate().get()).isEqualTo(LocalDate.of(2034, 3, 20));
        assertThat(snapshotAudits()).hasSize(1);
        assertThat(snapshotAudits().get(0).getSummary()).contains("reason").contains("owner").doesNotContain("Slow pickup").doesNotContain("Nok");

        tracker.updateSnapshot(entity.getId(), new BookingTrackerSnapshotUpdateInput().reason("Slow pickup").owner("Nok"));
        assertThat(snapshotAudits()).hasSize(1);

        // Omitted fields stay; null or blank clears; the frozen figures never move.
        BookingTrackerSnapshot cleared = tracker.updateSnapshot(entity.getId(), new BookingTrackerSnapshotUpdateInput().reason("   ").dueDate(null));
        assertThat(cleared.getReason().get()).isNull();
        assertThat(cleared.getNextAction().get()).isEqualTo("Call agents");
        assertThat(cleared.getDueDate().get()).isNull();
        assertThat(cleared.getRoomNights()).isEqualTo(entity.getRoomNights());
        assertThat(snapshotAudits()).hasSize(2);

        assertThatThrownBy(() -> tracker.updateSnapshot("missing", new BookingTrackerSnapshotUpdateInput().owner("x"))).isInstanceOf(NotFoundException.class);
    }

    // --- Rates ---------------------------------------------------------------------------------

    @Test
    void theServiceChargeRate_isAnAdminSetting_audited_andOnlyUsedByTheTracker() {
        BookingEntity b = stay("2037-11-21", "2037-11-26", BookingChannel.EXPEDIA, BookingStatus.CONFIRMED, 5, "6577.19");

        serviceChargeSettings.update(new ServiceChargeSettingsUpdateInput(BigDecimal.ZERO));
        BookingTrackerService.Figures f = row(tracker.computation("2037-11", "2037-11", null), b).months().get(YearMonth.of(2037, 11));
        assertThat(f.serviceCharge()).isEqualByComparingTo("0");
        assertThat(f.packageRevenue()).isEqualByComparingTo("30734.53"); // 32885.95 / 1.07
        assertThat(f.vat()).isEqualByComparingTo("2151.42");
        assertThat(tracker.report("2037-11", "2037-11", null).getServiceChargeRate()).isEqualTo("0.00");

        assertThat(auditsOf(AuditAction.SERVICE_CHARGE_SETTINGS_UPDATED)).hasSize(1);
        serviceChargeSettings.update(new ServiceChargeSettingsUpdateInput(new BigDecimal("0.0")));
        assertThat(auditsOf(AuditAction.SERVICE_CHARGE_SETTINGS_UPDATED)).hasSize(1); // unchanged: nothing written
        assertThatThrownBy(() -> serviceChargeSettings.update(new ServiceChargeSettingsUpdateInput(new BigDecimal("10.005"))))
                .isInstanceOf(ValidationException.class);
    }

    // --- Breakfast on the booking --------------------------------------------------------------

    @Test
    void breakfast_isSetCorrectedAndClearedThroughTheStatusForm_audited_andValidated() {
        BookingEntity b = stay("2037-11-10", "2037-11-12", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 2, "1000.00");
        b.setAdults(2);
        bookingRepository.saveAndFlush(b);

        var set = bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).breakfastGuests(2).breakfastRate(BreakfastRate.NUMBER_350));
        assertThat(set.getBreakfastGuests().get()).isEqualTo(2);
        assertThat(set.getBreakfastRate().get()).isEqualTo(BreakfastRate.NUMBER_350);
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getBreakfastRate()).isEqualTo(350);
        List<AuditLogEntity> audit = bookingAudits(b.getId());
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).getSummary()).contains("Breakfast changed from none to 2 guests at ฿350 per night");

        // Omitted leaves it; an unchanged value writes no entry.
        bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).breakfastGuests(2));
        bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).adults(2));
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getBreakfastRate()).isEqualTo(350);
        assertThat(bookingAudits(b.getId())).hasSize(1);

        // More than the party, or guests without a rate, is refused.
        assertThatThrownBy(() -> bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).breakfastGuests(3)))
                .isInstanceOf(ValidationException.class);
        BookingEntity other = stay("2037-11-14", "2037-11-15", BookingChannel.DIRECT, BookingStatus.CONFIRMED, 1, "1000.00");
        assertThatThrownBy(() -> bookingService.updateStatus(other.getId(), new BookingStatusInput(BookingStatus.CONFIRMED).breakfastGuests(1)))
                .isInstanceOf(ValidationException.class);

        // null clears both.
        var cleared = bookingService.updateStatus(b.getId(), new BookingStatusInput(BookingStatus.CONFIRMED)
                .breakfastGuests(null).breakfastRate(null));
        assertThat(cleared.getBreakfastGuests().isPresent() ? cleared.getBreakfastGuests().get() : null).isNull();
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getBreakfastGuests()).isNull();
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getBreakfastRate()).isNull();
    }

    // --- Market segment ------------------------------------------------------------------------

    @Test
    void theNewChannels_areOnlineAgentsInTheMarketSegmentReport() {
        long otaBefore = otaNights("2037-03-01", "2037-03-31");
        stay("2037-03-02", "2037-03-03", BookingChannel.CTRIP, BookingStatus.CONFIRMED, 1, "1000.00");
        stay("2037-03-04", "2037-03-06", BookingChannel.HOTELBEDS, BookingStatus.CONFIRMED, 2, "1000.00");
        stay("2037-03-08", "2037-03-11", BookingChannel.AGENT, BookingStatus.CONFIRMED, 3, "1000.00");
        assertThat(otaNights("2037-03-01", "2037-03-31") - otaBefore).isEqualTo(6);
    }

    // --- The workbook --------------------------------------------------------------------------

    @Test
    void theExport_hasTheWorkbooksSheetsAndFigures_chartsAndSafeText() throws Exception {
        BookingEntity expedia = stay("2037-11-21", "2037-11-26", BookingChannel.EXPEDIA, BookingStatus.CONFIRMED, 5, "6577.19");
        breakfast(expedia, 2, 250);
        BookingEntity hostile = stay("2037-12-02", "2037-12-04", BookingChannel.DIRECT, BookingStatus.PAID, 2, "1000.00");
        hostile.setGuestName("=HYPERLINK(\"http://evil.example\")");
        bookingRepository.saveAndFlush(hostile);
        persistUnit(true); // something to be available, so occupancy has a figure
        tracker.updateMonth("2037-11", new BookingTrackerMonthTargetsInput().targetRoomNights(100));
        snapshots.runOnce();

        BookingTrackerExportService.Export export = exporter.export("2037-11", "2037-12", null);
        assertThat(export.filename()).isEqualTo("booking-tracker_2037-11_2037-12_2034-03-10.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export.workbook()))) {
            List<String> names = new ArrayList<>();
            workbook.forEach(s -> names.add(s.getSheetName()));
            assertThat(names).containsExactly("Results", "Channel summary", "Booking trends", "Weekly review", "Bookings");

            // Results: the first table's rows are the two months and a Total, with the same numbers the screen gets.
            XSSFSheet results = workbook.getSheet("Results");
            int header = rowWith(results, "Results");
            assertThat(results.getRow(header + 1).getCell(0).getStringCellValue()).isEqualTo("2037-11");
            assertThat(results.getRow(header + 1).getCell(2).getNumericCellValue()).isEqualTo(5);            // booked room nights
            assertThat(results.getRow(header + 1).getCell(5).getNumericCellValue()).isEqualTo(2500.00);      // breakfast
            assertThat(results.getRow(header + 1).getCell(4).getNumericCellValue()).isEqualTo(25440.48);     // net room revenue
            assertThat(results.getRow(header + 1).getCell(7).getNumericCellValue()).isEqualTo(100);          // target
            assertThat(results.getRow(header + 3).getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(results.getRow(header + 1).getCell(4).getCellStyle().getDataFormatString()).contains("0.00");
            assertThat(results.getRow(header + 1).getCell(3).getCellStyle().getDataFormatString()).contains("%");

            // Charts: two on the channel summary, three on the trends sheet.
            assertThat(workbook.getSheet("Channel summary").getDrawingPatriarch().getCharts()).hasSize(2);
            assertThat(workbook.getSheet("Booking trends").getDrawingPatriarch().getCharts()).hasSize(3);

            // Bookings: one row per counted booking; the text a person typed cannot start a formula.
            XSSFSheet bookings = workbook.getSheet("Bookings");
            assertThat(bookings.getLastRowNum()).isGreaterThanOrEqualTo(2);
            boolean found = false;
            for (int r = 1; r <= bookings.getLastRowNum(); r++) {
                String guest = bookings.getRow(r).getCell(1).getStringCellValue();
                assertThat(guest).doesNotStartWith("=");
                if (guest.contains("HYPERLINK")) {
                    assertThat(guest).startsWith("'=");
                    found = true;
                }
            }
            assertThat(found).isTrue();
            assertThat(bookings.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Guest name");
            assertThat(bookings.getRow(0).getCell(21).getStringCellValue()).isEqualTo("Nov 2037 nights");
            assertThat(bookings.getRow(1).getCell(3).getCellType().name()).isEqualTo("NUMERIC"); // a date is a date

        }

        // Weekly review lists the frozen snapshots of the months asked for (the job wrote Nov 2033 - Apr 2034 and the season after).
        try (XSSFWorkbook review = new XSSFWorkbook(new ByteArrayInputStream(exporter.export("2033-12", "2033-12", null).workbook()))) {
            XSSFSheet sheet = review.getSheet("Weekly review");
            int header = rowWith(sheet, "Snapshot");
            assertThat(sheet.getLastRowNum()).isEqualTo(header + 1);
            assertThat(sheet.getRow(header + 1).getCell(1).getStringCellValue()).isEqualTo("2033-12");
            assertThat(sheet.getRow(header + 1).getCell(2).getStringCellValue()).isEqualTo("late");
        }
    }

    // --- Fixtures ------------------------------------------------------------------------------

    private static int rowWith(XSSFSheet sheet, String firstCell) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            if (sheet.getRow(r) != null && sheet.getRow(r).getCell(0) != null && firstCell.equals(sheet.getRow(r).getCell(0).getStringCellValue())) {
                return r;
            }
        }
        throw new AssertionError("No row starting with " + firstCell);
    }

    private static void assertFigures(BookingTrackerService.Figures f, long nights, String gross, String pkg, String sc, String vat, String breakfast, String net) {
        assertThat(f.nights()).isEqualTo(nights);
        assertThat(f.gross()).isEqualByComparingTo(gross);
        assertThat(f.packageRevenue()).isEqualByComparingTo(pkg);
        assertThat(f.serviceCharge()).isEqualByComparingTo(sc);
        assertThat(f.vat()).isEqualByComparingTo(vat);
        assertThat(f.breakfast()).isEqualByComparingTo(breakfast);
        assertThat(f.net()).isEqualByComparingTo(net);
    }

    private BookingTrackerService.BookingRow row(BookingTrackerService.Computation c, BookingEntity booking) {
        return c.rows().stream().filter(r -> r.booking().getId().equals(booking.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("Booking " + booking.getId() + " is not in the tracker"));
    }

    private BookingTrackerChannelRow channel(BookingTrackerReport report, BookingChannel channel) {
        return report.getChannels().getRows().stream().filter(r -> r.getChannel().get() == channel).findFirst().orElseThrow();
    }

    private int bookedNights(String month) {
        return tracker.report(month, month, null).getMonths().get(0).getBookedRoomNights();
    }

    private BookingTrackerSnapshotEntity snapshot(String date, String month) {
        return snapshotRepository.findByStayMonthIn(List.of(month)).stream()
                .filter(s -> s.getSnapshotDate().equals(LocalDate.parse(date))).findFirst()
                .orElseThrow(() -> new AssertionError("No snapshot " + date + " for " + month));
    }

    private long otaNights(String from, String to) {
        return reportService.marketSegment(from, to).getSegments().stream()
                .filter(s -> s.getSegment() == MarketSegment.OTA).mapToLong(MarketSegmentRow::getRoomNights).sum();
    }

    private List<AuditLogEntity> targetAudits(String month) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_TRACKER_TARGET_UPDATED && month.equals(e.getEntityId()))
                .sorted(java.util.Comparator.comparing(AuditLogEntity::getCreatedAt))
                .toList();
    }

    private List<AuditLogEntity> snapshotAudits() {
        return auditsOf(AuditAction.BOOKING_TRACKER_SNAPSHOT_UPDATED);
    }

    private List<AuditLogEntity> auditsOf(AuditAction action) {
        return auditLogRepository.findAll().stream().filter(e -> e.getAction() == action).toList();
    }

    private List<AuditLogEntity> bookingAudits(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_STATUS_CHANGED && bookingId.equals(e.getEntityId())).toList();
    }

    private void breakfast(BookingEntity booking, int guests, int rate) {
        booking.setBreakfastGuests(guests);
        booking.setBreakfastRate(rate);
        bookingRepository.saveAndFlush(booking);
    }

    /** Backdates the row's creation (a generated column) by SQL, then drops what the persistence context cached. */
    private void createdAtUtc(BookingEntity booking, String utc) {
        jdbcTemplate.update("UPDATE \"Booking\" SET \"createdAt\" = ?::timestamp WHERE \"id\" = ?", utc, booking.getId());
        entityManager.clear();
    }

    private void block(RoomUnitEntity unit, String from, String to) {
        RoomUnitBlockEntity block = new RoomUnitBlockEntity();
        block.setRoomUnitId(unit.getId());
        block.setFromDate(LocalDate.parse(from));
        block.setToDate(LocalDate.parse(to));
        block.setReason("Tracker test");
        block.setCreatedByUserId(user.getId());
        blockRepository.saveAndFlush(block);
    }

    private RoomEntity persistRoom() {
        RoomEntity r = new RoomEntity();
        r.setName("Tracker Room " + UUID.randomUUID());
        r.setDescription("Room used only by BookingTrackerTests");
        r.setCapacity(4);
        r.setBasePrice(new BigDecimal("1000.00"));
        return roomRepository.saveAndFlush(r);
    }

    private RoomUnitEntity persistUnit(boolean active) {
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(room.getId());
        unit.setLabel("TR-" + UUID.randomUUID().toString().substring(0, 8));
        unit.setActive(active);
        return roomUnitRepository.saveAndFlush(unit);
    }

    /**
     * A one-segment booking whose nights are frozen at {@code nightly} each (or at the explicit prices given). Four adults:
     * a breakfast of any size in the tests fits the party.
     */
    private BookingEntity stay(String checkIn, String checkOut, BookingChannel channel, BookingStatus status, int nights, String nightly, String... explicit) {
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        List<BigDecimal> prices = new ArrayList<>();
        if (explicit.length > 0) {
            for (String p : new String[] {nightly}) prices.add(new BigDecimal(p));
            for (String p : explicit) prices.add(new BigDecimal(p));
        } else {
            for (int i = 0; i < nights; i++) prices.add(new BigDecimal(nightly));
        }
        BigDecimal total = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BookingEntity booking = new BookingEntity();
        booking.setChannel(channel);
        booking.setOccupancyStatus(OccupancyStatus.EXPECTED);
        booking.setAdults(4);
        booking.setChildren(0);
        booking.setRoomId(room.getId());
        booking.setGuestName("Tracker Guest");
        booking.setCheckIn(in);
        booking.setCheckOut(out);
        booking.setTotalPrice(total);
        booking.setStatus(status);
        booking = bookingRepository.saveAndFlush(booking);

        BookingSegmentEntity segment = new BookingSegmentEntity();
        segment.setBookingId(booking.getId());
        segment.setRoomId(room.getId());
        segment.setCheckIn(in);
        segment.setCheckOut(out);
        segment.setTotalPrice(total);
        segment = segmentRepository.saveAndFlush(segment);
        for (int i = 0; i < prices.size(); i++) {
            BookingSegmentNightlyRateEntity rate = new BookingSegmentNightlyRateEntity();
            rate.setSegmentId(segment.getId());
            rate.setDate(in.plusDays(i));
            rate.setPrice(prices.get(i));
            nightlyRateRepository.save(rate);
        }
        nightlyRateRepository.flush();
        return booking;
    }
}
