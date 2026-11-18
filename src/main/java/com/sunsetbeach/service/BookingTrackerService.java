package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSegmentNightlyRateEntity;
import com.sunsetbeach.entity.BookingTrackerMonthEntity;
import com.sunsetbeach.entity.BookingTrackerSnapshotEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingTrackerBucket;
import com.sunsetbeach.model.BookingTrackerChannelRank;
import com.sunsetbeach.model.BookingTrackerChannelRow;
import com.sunsetbeach.model.BookingTrackerChannels;
import com.sunsetbeach.model.BookingTrackerMonth;
import com.sunsetbeach.model.BookingTrackerMonthTargets;
import com.sunsetbeach.model.BookingTrackerMonthTargetsInput;
import com.sunsetbeach.model.BookingTrackerReport;
import com.sunsetbeach.model.BookingTrackerSnapshot;
import com.sunsetbeach.model.BookingTrackerSnapshotUpdateInput;
import com.sunsetbeach.model.BookingTrackerTrends;
import com.sunsetbeach.model.BookingTrackerWeek;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingSegmentNightlyRateRepository;
import com.sunsetbeach.repository.BookingTrackerMonthRepository;
import com.sunsetbeach.repository.BookingTrackerSnapshotRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The booking tracker ({@code GET /reports/booking-tracker}): the hotel's hand-filled Excel workbook computed from
 * real data. Every definition is in that operation's openapi description; the notes here are about how.
 *
 * <p><b>One population of sold nights.</b> The nights come from {@link ReportService#segmentsInRange} - the slices
 * every other room report counts (segment nights and the nights of a guest staying past check-out) - narrowed to
 * counted bookings ({@code CONFIRMED}/{@code PAID}, not a no-show) and then priced night by night from the frozen
 * {@code BookingSegmentNightlyRate} (an overstay night at the price charged to the booking). There is no second
 * definition of "a sold night" here, only a status filter and a per-night price.
 *
 * <p><b>Rounding.</b> Per booking and stay month, each step is rounded to two decimals ({@link #figures}): package,
 * service charge, VAT (the remainder), breakfast, net. Months, channels, buckets and weeks only ever add those
 * rounded figures, so every table agrees with every other to the cent. The service charge is deducted for this
 * analysis only; nothing is charged to anyone.
 *
 * <p>The export ({@code BookingTrackerExportService}) and the weekly snapshot job read {@link #computation}, so there
 * is one copy of the arithmetic.
 */
@Service
public class BookingTrackerService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Booking-window buckets: label, first day, last day (null = open-ended). */
    static final List<Bucket> BUCKETS = List.of(
            new Bucket("0-3", 0, 3),
            new Bucket("4-7", 4, 7),
            new Bucket("8-14", 8, 14),
            new Bucket("15-30", 15, 30),
            new Bucket("31-60", 31, 60),
            new Bucket("61-90", 61, 90),
            new Bucket("91+", 91, null));

    static final int PACE_WEEKS = 12;

    record Bucket(String label, int from, Integer to) {
        boolean contains(long days) {
            return days >= from && (to == null || days <= to);
        }
    }

    /** One booking's money for the nights of one stay month, every step already rounded. */
    record Figures(long nights, BigDecimal gross, BigDecimal packageRevenue, BigDecimal serviceCharge, BigDecimal vat,
                   BigDecimal breakfast, BigDecimal net) {
        static final Figures ZERO = new Figures(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        Figures plus(Figures o) {
            return new Figures(nights + o.nights, gross.add(o.gross), packageRevenue.add(o.packageRevenue), serviceCharge.add(o.serviceCharge),
                    vat.add(o.vat), breakfast.add(o.breakfast), net.add(o.net));
        }
    }

    /** A counted booking with its figures per stay month of the range, and what the trends need. */
    record BookingRow(BookingEntity booking, String roomName, LocalDate bookingDate, long windowDays, Map<YearMonth, Figures> months) {
        Figures total(List<YearMonth> inMonths) {
            Figures sum = Figures.ZERO;
            for (YearMonth m : inMonths) {
                Figures f = months.get(m);
                if (f != null) sum = sum.plus(f);
            }
            return sum;
        }
    }

    /** The report plus the booking rows behind it (the export's Bookings sheet). */
    record Computation(BookingTrackerPeriod period, LocalDate today, BookingTrackerReport report, List<BookingRow> rows) {}

    private final ReportService reportService;
    private final BookingSegmentNightlyRateRepository nightlyRateRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository blockRepository;
    private final BookingTrackerMonthRepository monthRepository;
    private final BookingTrackerSnapshotRepository snapshotRepository;
    private final VatSettingsService vatSettingsService;
    private final ServiceChargeSettingsService serviceChargeSettingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public BookingTrackerService(
            ReportService reportService,
            BookingSegmentNightlyRateRepository nightlyRateRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository blockRepository,
            BookingTrackerMonthRepository monthRepository,
            BookingTrackerSnapshotRepository snapshotRepository,
            VatSettingsService vatSettingsService,
            ServiceChargeSettingsService serviceChargeSettingsService,
            AuditLogService auditLogService,
            Clock clock) {
        this.reportService = reportService;
        this.nightlyRateRepository = nightlyRateRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.blockRepository = blockRepository;
        this.monthRepository = monthRepository;
        this.snapshotRepository = snapshotRepository;
        this.vatSettingsService = vatSettingsService;
        this.serviceChargeSettingsService = serviceChargeSettingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    // --- GET /reports/booking-tracker ---------------------------------------------------------

    @Transactional(readOnly = true)
    public BookingTrackerReport report(String fromMonth, String toMonth, String stayMonth) {
        return computation(fromMonth, toMonth, stayMonth).report();
    }

    @Transactional(readOnly = true)
    public Computation computation(String fromMonth, String toMonth, String stayMonth) {
        LocalDate today = LocalDate.now(clock);
        return computation(BookingTrackerPeriod.parse(fromMonth, toMonth, stayMonth, today), today);
    }

    /** Package-private and not itself transactional: callers outside this class wrap it in their own read-only transaction. */
    Computation computation(BookingTrackerPeriod period, LocalDate today) {
        BigDecimal vatRate = vatSettingsService.currentRate();
        BigDecimal serviceChargeRate = serviceChargeSettingsService.currentRate();
        List<YearMonth> months = period.months();

        List<BookingRow> rows = bookingRows(period, vatRate, serviceChargeRate);

        // Inputs and availability.
        Set<String> activeUnitIds = roomUnitRepository.findAll().stream()
                .filter(RoomUnitEntity::isActive).map(RoomUnitEntity::getId).collect(Collectors.toSet());
        Map<YearMonth, Long> blocked = blockedRoomNights(period, activeUnitIds);
        Map<String, BookingTrackerMonthEntity> entered = monthRepository
                .findAllById(months.stream().map(YearMonth::toString).toList()).stream()
                .collect(Collectors.toMap(BookingTrackerMonthEntity::getMonth, e -> e));

        List<MonthTotals> monthTotals = new ArrayList<>();
        for (YearMonth m : months) {
            Figures sum = Figures.ZERO;
            for (BookingRow row : rows) {
                Figures f = row.months().get(m);
                if (f != null) sum = sum.plus(f);
            }
            monthTotals.add(new MonthTotals(m, activeUnitIds.size(), m.lengthOfMonth(), blocked.getOrDefault(m, 0L), sum, entered.get(m.toString())));
        }

        List<BookingTrackerMonth> monthDtos = monthTotals.stream().map(MonthTotals::toDto).toList();
        BookingTrackerMonth total = totalRow(monthTotals);

        List<YearMonth> filter = period.filterMonths();
        BookingTrackerReport report = new BookingTrackerReport(
                period.from().toString(),
                period.to().toString(),
                period.stayMonth() != null ? period.stayMonth().toString() : null,
                today,
                PriceFormat.asDecimalString(vatRate),
                PriceFormat.asDecimalString(serviceChargeRate),
                monthDtos,
                total,
                channels(rows, filter),
                trends(rows, filter, today),
                weeklyReview(months));
        return new Computation(period, today, report, rows);
    }

    // --- Booking rows --------------------------------------------------------------------------

    private List<BookingRow> bookingRows(BookingTrackerPeriod period, BigDecimal vatRate, BigDecimal serviceChargeRate) {
        ReportDateRange range = new ReportDateRange(period.firstDay(), period.lastDay());
        List<ReportService.SegmentInRange> slices = reportService.segmentsInRange(range);
        Map<String, BookingEntity> bookings = reportService.bookingsOf(slices);

        List<ReportService.SegmentInRange> counted = slices.stream()
                .filter(s -> isCounted(bookings.get(s.segment().getBookingId())))
                .toList();

        Map<String, Map<LocalDate, BigDecimal>> ratesBySegment = new HashMap<>();
        List<String> segmentIds = counted.stream().filter(s -> !s.isOverstay()).map(s -> s.segment().getId()).distinct().toList();
        if (!segmentIds.isEmpty()) {
            for (BookingSegmentNightlyRateEntity rate : nightlyRateRepository.findBySegmentIdIn(segmentIds)) {
                ratesBySegment.computeIfAbsent(rate.getSegmentId(), k -> new HashMap<>()).put(rate.getDate(), rate.getPrice());
            }
        }

        // booking -> stay month -> (nights, gross)
        Map<String, Map<YearMonth, long[]>> nights = new LinkedHashMap<>();
        Map<String, Map<YearMonth, BigDecimal>> gross = new HashMap<>();
        for (ReportService.SegmentInRange slice : counted) {
            String bookingId = slice.segment().getBookingId();
            if (slice.isOverstay()) {
                for (Map.Entry<LocalDate, BigDecimal> night : slice.overstayNightPrices().entrySet()) {
                    if (night.getKey().isBefore(range.from()) || night.getKey().isAfter(range.to())) continue;
                    addNight(nights, gross, bookingId, night.getKey(), night.getValue());
                }
                continue;
            }
            BookingSegmentEntity segment = slice.segment();
            long segmentNights = ChronoUnit.DAYS.between(segment.getCheckIn(), segment.getCheckOut());
            // Only a booking from before nightly rates existed has no row for a night: spread its segment evenly.
            BigDecimal evenly = segment.getTotalPrice().divide(BigDecimal.valueOf(segmentNights), 2, RoundingMode.HALF_UP);
            Map<LocalDate, BigDecimal> rates = ratesBySegment.getOrDefault(segment.getId(), Map.of());
            for (LocalDate d = slice.from(); d.isBefore(slice.toExclusive()); d = d.plusDays(1)) {
                addNight(nights, gross, bookingId, d, rates.getOrDefault(d, evenly));
            }
        }

        Map<String, String> roomNames = roomRepository.findAllById(
                        nights.keySet().stream().map(id -> bookings.get(id).getRoomId()).distinct().toList())
                .stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));

        List<BookingRow> rows = new ArrayList<>();
        for (Map.Entry<String, Map<YearMonth, long[]>> entry : nights.entrySet()) {
            BookingEntity booking = bookings.get(entry.getKey());
            Map<YearMonth, Figures> byMonth = new TreeMap<>();
            for (Map.Entry<YearMonth, long[]> month : entry.getValue().entrySet()) {
                byMonth.put(month.getKey(), figures(booking, month.getValue()[0], gross.get(entry.getKey()).get(month.getKey()), vatRate, serviceChargeRate));
            }
            LocalDate bookingDate = bookingDate(booking);
            long window = Math.max(0, ChronoUnit.DAYS.between(bookingDate, booking.getCheckIn()));
            rows.add(new BookingRow(booking, roomNames.getOrDefault(booking.getRoomId(), ""), bookingDate, window, byMonth));
        }
        rows.sort(Comparator.comparing((BookingRow r) -> r.booking().getCheckIn()).thenComparing(r -> r.booking().getId()));
        return rows;
    }

    private static void addNight(Map<String, Map<YearMonth, long[]>> nights, Map<String, Map<YearMonth, BigDecimal>> gross,
                                 String bookingId, LocalDate night, BigDecimal price) {
        YearMonth month = YearMonth.from(night);
        nights.computeIfAbsent(bookingId, k -> new TreeMap<>()).computeIfAbsent(month, k -> new long[1])[0]++;
        gross.computeIfAbsent(bookingId, k -> new HashMap<>()).merge(month, price, BigDecimal::add);
    }

    /** {@code CONFIRMED} or {@code PAID} (the workbook's "Confirmed"), and not a no-show. */
    static boolean isCounted(BookingEntity booking) {
        return (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PAID)
                && booking.getOccupancyStatus() != OccupancyStatus.NO_SHOW;
    }

    /**
     * One booking's money for one stay month - the workbook's formulas, each step rounded to two decimals:
     * package = gross / (1 + SC) / (1 + VAT); service charge = package x SC; VAT = gross - package - service charge;
     * breakfast = nights x guests x rate; net = package - breakfast.
     */
    static Figures figures(BookingEntity booking, long nights, BigDecimal gross, BigDecimal vatPercent, BigDecimal serviceChargePercent) {
        BigDecimal scFactor = BigDecimal.ONE.add(serviceChargePercent.divide(HUNDRED, 10, RoundingMode.UNNECESSARY));
        BigDecimal vatFactor = BigDecimal.ONE.add(vatPercent.divide(HUNDRED, 10, RoundingMode.UNNECESSARY));
        BigDecimal grossRounded = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pkg = grossRounded.divide(scFactor, 12, RoundingMode.HALF_UP).divide(vatFactor, 12, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceCharge = pkg.multiply(serviceChargePercent).divide(HUNDRED, 12, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = grossRounded.subtract(pkg).subtract(serviceCharge);
        BigDecimal breakfast = BigDecimal.ZERO.setScale(2);
        if (booking.getBreakfastGuests() != null && booking.getBreakfastRate() != null) {
            breakfast = BigDecimal.valueOf(nights)
                    .multiply(BigDecimal.valueOf(booking.getBreakfastGuests()))
                    .multiply(BigDecimal.valueOf(booking.getBreakfastRate()))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new Figures(nights, grossRounded, pkg, serviceCharge, vat, breakfast, pkg.subtract(breakfast));
    }

    /** The hotel day the booking was made: SiteMinder's own "booked on" when stored, else the row's creation (both UTC wall-clock). */
    LocalDate bookingDate(BookingEntity booking) {
        var utc = booking.getBookedAt() != null ? booking.getBookedAt() : booking.getCreatedAt();
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate();
    }

    // --- Availability --------------------------------------------------------------------------

    /** Nights each stay month a unit is out of service; a unit under overlapping blocks counts once per night. */
    private Map<YearMonth, Long> blockedRoomNights(BookingTrackerPeriod period, Set<String> activeUnitIds) {
        LocalDate from = period.firstDay();
        LocalDate to = period.lastDay();
        Map<String, Set<LocalDate>> nightsByUnit = new HashMap<>();
        for (RoomUnitBlockEntity block : blockRepository.findByFromDateLessThanEqualAndToDateGreaterThanEqual(to, from)) {
            if (!activeUnitIds.contains(block.getRoomUnitId())) continue;
            LocalDate start = block.getFromDate().isBefore(from) ? from : block.getFromDate();
            LocalDate end = block.getToDate().isAfter(to) ? to : block.getToDate();
            Set<LocalDate> nights = nightsByUnit.computeIfAbsent(block.getRoomUnitId(), k -> new HashSet<>());
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                nights.add(d);
            }
        }
        Map<YearMonth, Long> byMonth = new HashMap<>();
        for (Set<LocalDate> nights : nightsByUnit.values()) {
            for (LocalDate d : nights) {
                byMonth.merge(YearMonth.from(d), 1L, Long::sum);
            }
        }
        return byMonth;
    }

    /** One stay month's figures and inputs, before they become a DTO. */
    private record MonthTotals(YearMonth month, int physicalRooms, int nightsInMonth, long blocked, Figures figures, BookingTrackerMonthEntity entered) {
        long available() {
            return (long) physicalRooms * nightsInMonth - blocked;
        }

        BookingTrackerMonth toDto() {
            Integer target = entered != null ? entered.getTargetRoomNights() : null;
            Integer baseline = entered != null ? entered.getBaselineRoomNights() : null;
            BigDecimal baselineNet = entered != null ? entered.getBaselineNetRevenue() : null;
            return dto(month.toString(), physicalRooms, nightsInMonth, blocked, available(), figures,
                    baseline, baselineNet, target,
                    target == null ? null : (int) Math.max(0, target - figures.nights()),
                    baseline == null ? null : (int) (figures.nights() - baseline));
        }
    }

    private static BookingTrackerMonth dto(String month, Integer physicalRooms, int nightsInMonth, long blocked, long available, Figures f,
                                           Integer baselineNights, BigDecimal baselineNet, Integer target, Integer stillNeeded, Integer pickup) {
        return new BookingTrackerMonth(
                month,
                physicalRooms,
                nightsInMonth,
                Math.toIntExact(blocked),
                Math.toIntExact(available),
                Math.toIntExact(f.nights()),
                percent(BigDecimal.valueOf(f.nights()), BigDecimal.valueOf(available)),
                money(f.gross()),
                money(f.packageRevenue()),
                money(f.serviceCharge()),
                money(f.vat()),
                money(f.breakfast()),
                money(f.net()),
                f.nights() == 0 ? null : money(f.net().divide(BigDecimal.valueOf(f.nights()), 12, RoundingMode.HALF_UP)),
                baselineNights,
                baselineNet != null ? money(baselineNet) : null,
                target,
                stillNeeded,
                pickup);
    }

    /** The months added up; inputs add up over the months that have them (null when none does). */
    private static BookingTrackerMonth totalRow(List<MonthTotals> months) {
        Figures sum = Figures.ZERO;
        int nights = 0;
        long blocked = 0;
        long available = 0;
        Integer target = null;
        Integer baselineNights = null;
        BigDecimal baselineNet = null;
        Integer stillNeeded = null;
        Integer pickup = null;
        for (MonthTotals m : months) {
            sum = sum.plus(m.figures());
            nights += m.nightsInMonth();
            blocked += m.blocked();
            available += m.available();
            BookingTrackerMonth dto = m.toDto();
            if (dto.getTargetRoomNights().isPresent() && dto.getTargetRoomNights().get() != null) {
                target = (target == null ? 0 : target) + dto.getTargetRoomNights().get();
                stillNeeded = (stillNeeded == null ? 0 : stillNeeded) + dto.getStillNeededRoomNights().get();
            }
            if (m.entered() != null && m.entered().getBaselineRoomNights() != null) {
                baselineNights = (baselineNights == null ? 0 : baselineNights) + m.entered().getBaselineRoomNights();
                pickup = (pickup == null ? 0 : pickup) + dto.getPickupVsBaseline().get();
            }
            if (m.entered() != null && m.entered().getBaselineNetRevenue() != null) {
                baselineNet = (baselineNet == null ? BigDecimal.ZERO : baselineNet).add(m.entered().getBaselineNetRevenue());
            }
        }
        return dto(null, null, nights, blocked, available, sum, baselineNights, baselineNet, target, stillNeeded, pickup);
    }

    // --- Channel summary -----------------------------------------------------------------------

    private BookingTrackerChannels channels(List<BookingRow> rows, List<YearMonth> filter) {
        Map<BookingChannel, Figures> byChannel = new EnumMap<>(BookingChannel.class);
        for (BookingChannel c : BookingChannel.values()) byChannel.put(c, Figures.ZERO);
        Figures total = Figures.ZERO;
        for (BookingRow row : rows) {
            Figures f = row.total(filter);
            if (f.nights() == 0) continue;
            byChannel.merge(row.booking().getChannel(), f, Figures::plus);
            total = total.plus(f);
        }
        List<BookingTrackerChannelRow> channelRows = new ArrayList<>();
        for (Map.Entry<BookingChannel, Figures> e : byChannel.entrySet()) {
            channelRows.add(channelRow(e.getKey(), e.getValue(), total));
        }
        List<BookingTrackerChannelRank> ranking = new ArrayList<>();
        List<Map.Entry<BookingChannel, Figures>> ranked = byChannel.entrySet().stream()
                .filter(e -> e.getValue().nights() > 0)
                .sorted(Comparator.comparing((Map.Entry<BookingChannel, Figures> e) -> e.getValue().net()).reversed()
                        .thenComparing(Comparator.comparingLong((Map.Entry<BookingChannel, Figures> e) -> e.getValue().nights()).reversed()))
                .toList();
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<BookingChannel, Figures> e = ranked.get(i);
            ranking.add(new BookingTrackerChannelRank(i + 1, e.getKey(), money(e.getValue().net()), Math.toIntExact(e.getValue().nights())));
        }
        return new BookingTrackerChannels(channelRows, channelRow(null, total, total), ranking);
    }

    private static BookingTrackerChannelRow channelRow(BookingChannel channel, Figures f, Figures total) {
        return new BookingTrackerChannelRow(
                channel,
                Math.toIntExact(f.nights()),
                money(f.net()),
                f.nights() == 0 ? null : money(f.net().divide(BigDecimal.valueOf(f.nights()), 12, RoundingMode.HALF_UP)),
                percent(BigDecimal.valueOf(f.nights()), BigDecimal.valueOf(total.nights())),
                percent(f.net(), total.net()));
    }

    // --- Booking trends ------------------------------------------------------------------------

    private BookingTrackerTrends trends(List<BookingRow> rows, List<YearMonth> filter, LocalDate today) {
        long[] bucketNights = new long[BUCKETS.size()];
        BigDecimal[] bucketNet = new BigDecimal[BUCKETS.size()];
        java.util.Arrays.fill(bucketNet, BigDecimal.ZERO);
        long totalNights = 0;
        BigDecimal windowTimesNights = BigDecimal.ZERO;

        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstMonday = thisMonday.minusWeeks(PACE_WEEKS - 1);
        long[] weekNights = new long[PACE_WEEKS];
        BigDecimal[] weekNet = new BigDecimal[PACE_WEEKS];
        java.util.Arrays.fill(weekNet, BigDecimal.ZERO);

        for (BookingRow row : rows) {
            Figures f = row.total(filter);
            if (f.nights() == 0) continue;
            totalNights += f.nights();
            windowTimesNights = windowTimesNights.add(BigDecimal.valueOf(row.windowDays()).multiply(BigDecimal.valueOf(f.nights())));
            for (int i = 0; i < BUCKETS.size(); i++) {
                if (BUCKETS.get(i).contains(row.windowDays())) {
                    bucketNights[i] += f.nights();
                    bucketNet[i] = bucketNet[i].add(f.net());
                    break;
                }
            }
            if (!row.bookingDate().isBefore(firstMonday) && row.bookingDate().isBefore(thisMonday.plusWeeks(1))) {
                int week = (int) (ChronoUnit.DAYS.between(firstMonday, row.bookingDate()) / 7);
                weekNights[week] += f.nights();
                weekNet[week] = weekNet[week].add(f.net());
            }
        }

        List<BookingTrackerBucket> buckets = new ArrayList<>();
        for (int i = 0; i < BUCKETS.size(); i++) {
            Bucket b = BUCKETS.get(i);
            buckets.add(new BookingTrackerBucket(b.label(), b.from(), b.to(), Math.toIntExact(bucketNights[i]), money(bucketNet[i]),
                    percent(BigDecimal.valueOf(bucketNights[i]), BigDecimal.valueOf(totalNights))));
        }
        List<BookingTrackerWeek> weeks = new ArrayList<>();
        for (int i = 0; i < PACE_WEEKS; i++) {
            weeks.add(new BookingTrackerWeek(firstMonday.plusWeeks(i), Math.toIntExact(weekNights[i]), money(weekNet[i])));
        }
        return new BookingTrackerTrends(
                totalNights == 0 ? null : money(windowTimesNights.divide(BigDecimal.valueOf(totalNights), 12, RoundingMode.HALF_UP)),
                buckets,
                weeks);
    }

    // --- Weekly review -------------------------------------------------------------------------

    private List<BookingTrackerSnapshot> weeklyReview(List<YearMonth> months) {
        List<BookingTrackerSnapshotEntity> all = snapshotRepository.findByStayMonthIn(months.stream().map(YearMonth::toString).toList());
        List<BookingTrackerSnapshot> out = new ArrayList<>();
        Map<String, List<BookingTrackerSnapshotEntity>> byMonth = all.stream()
                .collect(Collectors.groupingBy(BookingTrackerSnapshotEntity::getStayMonth));
        for (List<BookingTrackerSnapshotEntity> list : byMonth.values()) {
            list.sort(Comparator.comparing(BookingTrackerSnapshotEntity::getSnapshotDate));
            BookingTrackerSnapshotEntity previous = null;
            for (BookingTrackerSnapshotEntity s : list) {
                out.add(snapshotDto(s, previous));
                previous = s;
            }
        }
        out.sort(Comparator.comparing(BookingTrackerSnapshot::getSnapshotDate).reversed().thenComparing(BookingTrackerSnapshot::getStayMonth));
        return out;
    }

    private static BookingTrackerSnapshot snapshotDto(BookingTrackerSnapshotEntity s, BookingTrackerSnapshotEntity previous) {
        return new BookingTrackerSnapshot(
                s.getId(),
                s.getSnapshotDate(),
                s.getStayMonth(),
                s.isLate(),
                s.getRoomNights(),
                money(s.getNetRoomRevenue()),
                s.getOccupancyPercent() != null ? money(s.getOccupancyPercent()) : null,
                previous != null ? s.getRoomNights() - previous.getRoomNights() : null,
                previous != null ? money(s.getNetRoomRevenue().subtract(previous.getNetRoomRevenue())) : null,
                s.getReason(),
                s.getNextAction(),
                s.getOwner(),
                s.getDueDate());
    }

    // --- PUT /reports/booking-tracker/months/{month} --------------------------------------------

    @Transactional
    public BookingTrackerMonthTargets updateMonth(String month, BookingTrackerMonthTargetsInput input) {
        String key = BookingTrackerPeriod.parseMonth("month", month).toString();
        Integer target = valueOrNull(input.getTargetRoomNights());
        Integer baselineNights = valueOrNull(input.getBaselineRoomNights());
        BigDecimal baselineNet = parseMoney(valueOrNull(input.getBaselineNetRevenue()));

        BookingTrackerMonthEntity entity = monthRepository.findById(key).orElse(null);
        Integer oldTarget = entity != null ? entity.getTargetRoomNights() : null;
        Integer oldBaselineNights = entity != null ? entity.getBaselineRoomNights() : null;
        BigDecimal oldBaselineNet = entity != null ? entity.getBaselineNetRevenue() : null;
        boolean unchanged = Objects.equals(oldTarget, target) && Objects.equals(oldBaselineNights, baselineNights)
                && (oldBaselineNet == null ? baselineNet == null : baselineNet != null && oldBaselineNet.compareTo(baselineNet) == 0);

        if (!unchanged) {
            if (target == null && baselineNights == null && baselineNet == null) {
                monthRepository.delete(entity);
            } else {
                if (entity == null) entity = new BookingTrackerMonthEntity(key);
                entity.setTargetRoomNights(target);
                entity.setBaselineRoomNights(baselineNights);
                entity.setBaselineNetRevenue(baselineNet);
                monthRepository.saveAndFlush(entity);
            }
            auditLogService.record(
                    AuditAction.BOOKING_TRACKER_TARGET_UPDATED,
                    AuditEntityType.SETTINGS,
                    key,
                    "Booking tracker " + key + ": target " + nightsText(oldTarget) + " -> " + nightsText(target)
                            + ", baseline room nights " + nightsText(oldBaselineNights) + " -> " + nightsText(baselineNights)
                            + ", baseline net revenue " + bahtText(oldBaselineNet) + " -> " + bahtText(baselineNet));
        }
        return new BookingTrackerMonthTargets(key, target, baselineNights, baselineNet != null ? money(baselineNet) : null);
    }

    // --- PATCH /reports/booking-tracker/snapshots/{id} -----------------------------------------

    @Transactional
    public BookingTrackerSnapshot updateSnapshot(String id, BookingTrackerSnapshotUpdateInput input) {
        BookingTrackerSnapshotEntity entity = snapshotRepository.findById(id).orElseThrow(() -> new NotFoundException("Snapshot not found"));
        List<String> changed = new ArrayList<>();
        if (input.getReason().isPresent()) {
            String v = cleanText(input.getReason().get());
            if (!Objects.equals(v, entity.getReason())) {
                entity.setReason(v);
                changed.add("reason");
            }
        }
        if (input.getNextAction().isPresent()) {
            String v = cleanText(input.getNextAction().get());
            if (!Objects.equals(v, entity.getNextAction())) {
                entity.setNextAction(v);
                changed.add("next action");
            }
        }
        if (input.getOwner().isPresent()) {
            String v = cleanText(input.getOwner().get());
            if (!Objects.equals(v, entity.getOwner())) {
                entity.setOwner(v);
                changed.add("owner");
            }
        }
        if (input.getDueDate().isPresent()) {
            LocalDate v = input.getDueDate().get();
            if (!Objects.equals(v, entity.getDueDate())) {
                entity.setDueDate(v);
                changed.add("due date");
            }
        }
        if (!changed.isEmpty()) {
            snapshotRepository.saveAndFlush(entity);
            // Names the fields, never their text - the notes are free text people type.
            auditLogService.record(
                    AuditAction.BOOKING_TRACKER_SNAPSHOT_UPDATED,
                    AuditEntityType.SETTINGS,
                    entity.getId(),
                    "Booking tracker snapshot " + entity.getSnapshotDate() + " for " + entity.getStayMonth() + ": " + String.join(", ", changed) + " changed");
        }
        BookingTrackerSnapshotEntity previous = snapshotRepository.findByStayMonthIn(List.of(entity.getStayMonth())).stream()
                .filter(s -> s.getSnapshotDate().isBefore(entity.getSnapshotDate()))
                .max(Comparator.comparing(BookingTrackerSnapshotEntity::getSnapshotDate))
                .orElse(null);
        return snapshotDto(entity, previous);
    }

    // --- Helpers -------------------------------------------------------------------------------

    private static <T> T valueOrNull(org.openapitools.jackson.nullable.JsonNullable<T> value) {
        return value.isPresent() ? value.get() : null;
    }

    private static BigDecimal parseMoney(String value) {
        if (value == null) return null;
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.scale() > 2) {
            throw ValidationException.field("baselineNetRevenue", "At most two decimals");
        }
        return parsed.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String cleanText(String text) {
        if (text == null) return null;
        String stripped = com.sunsetbeach.util.NameText.strip(text);
        return stripped.isEmpty() ? null : stripped;
    }

    private static String nightsText(Integer value) {
        return value == null ? "none" : String.valueOf(value);
    }

    private static String bahtText(BigDecimal value) {
        return value == null ? "none" : PriceFormat.baht(value);
    }

    static String money(BigDecimal value) {
        return PriceFormat.asDecimalString(value.setScale(2, RoundingMode.HALF_UP));
    }

    /** part / whole x 100, two decimals; null when whole is zero. */
    static String percent(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? null : money(part.multiply(HUNDRED).divide(whole, 12, RoundingMode.HALF_UP));
    }
}
