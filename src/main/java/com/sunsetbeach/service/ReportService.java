package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.ForecastDay;
import com.sunsetbeach.model.ForecastReport;
import com.sunsetbeach.model.ForecastRow;
import com.sunsetbeach.model.GuestLtvReport;
import com.sunsetbeach.model.GuestLtvRow;
import com.sunsetbeach.model.InHouseReport;
import com.sunsetbeach.model.InHouseRow;
import com.sunsetbeach.model.InHouseTotal;
import com.sunsetbeach.model.ManagerAccountCount;
import com.sunsetbeach.model.ManagerForecast;
import com.sunsetbeach.model.ManagerGuestStatistic;
import com.sunsetbeach.model.ManagerReport;
import com.sunsetbeach.model.ManagerReportDay;
import com.sunsetbeach.model.ManagerRevenue;
import com.sunsetbeach.model.ManagerRoomStatistic;
import com.sunsetbeach.model.MarketSegment;
import com.sunsetbeach.model.MarketSegmentReport;
import com.sunsetbeach.model.MarketSegmentRow;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyReport;
import com.sunsetbeach.model.OccupancyReportRow;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PosSalesMixCategory;
import com.sunsetbeach.model.PosSalesMixDepartment;
import com.sunsetbeach.model.PosSalesMixItem;
import com.sunsetbeach.model.PosSalesMixReport;
import com.sunsetbeach.model.RevenueCode;
import com.sunsetbeach.model.RevenueStatisticReport;
import com.sunsetbeach.model.RevenueStatisticRow;
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
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The read-only dashboard reports under {@code /reports}: occupancy/ADR/RevPAR, top production,
 * market segment, POS sales mix, guest lifetime value, the in-house list, the manager report, the forecast and the revenue statistic. Each operation's own openapi.yaml description is the contract;
 * notes here are only about how the numbers are computed.
 *
 * <p>Money is summed unrounded and rounded to two decimals ({@link RoundingMode#HALF_UP}) exactly
 * once, when it is rendered - prorated room revenue and the ratios built on it are the only
 * figures that aren't already whole cents.
 */
@Service
public class ReportService {

    /** Precision for the prorated/ratio intermediates - far more than two rendered decimals need. */
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BookingSegmentRepository segmentRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository blockRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final VatSettingsService vatSettingsService;
    private final Clock clock;
    private final OverstayRule overstayRule;

    public ReportService(
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository blockRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            BookingRepository bookingRepository,
            GuestRepository guestRepository,
            VatSettingsService vatSettingsService,
            Clock clock,
            OverstayRule overstayRule) {
        this.overstayRule = overstayRule;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.blockRepository = blockRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.bookingRepository = bookingRepository;
        this.guestRepository = guestRepository;
        this.vatSettingsService = vatSettingsService;
        this.clock = clock;
    }

    // --- GET /reports/occupancy ---------------------------------------------------------------

    /**
     * Segments, not bookings, and only {@code CANCELLED} excluded - the same population
     * {@link AvailabilityService}/{@link BookingCalendarService} treat as occupying a room type.
     * Grouped by {@code roomId}, never {@code roomUnitId}: an unassigned segment still occupies
     * one unit of its type.
     */
    @Transactional(readOnly = true)
    public OccupancyReport occupancy(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        long nights = range.days();

        Map<String, Long> activeUnits = roomUnitRepository.countActiveGroupedByRoom().stream()
                .collect(Collectors.toMap(RoomUnitRepository.RoomActiveUnitCount::getRoomId, RoomUnitRepository.RoomActiveUnitCount::getActiveCount));

        Map<String, long[]> soldByRoom = new HashMap<>();
        Map<String, BigDecimal> revenueByRoom = new HashMap<>();
        for (SegmentInRange slice : segmentsInRange(range)) {
            soldByRoom.computeIfAbsent(slice.segment().getRoomId(), k -> new long[1])[0] += slice.nights();
            revenueByRoom.merge(slice.segment().getRoomId(), slice.revenue(), BigDecimal::add);
        }

        List<OccupancyReportRow> rows = new ArrayList<>();
        long totalUnits = 0, totalSold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (RoomEntity room : roomRepository.findAll()) {
            long units = activeUnits.getOrDefault(room.getId(), 0L);
            long sold = soldByRoom.containsKey(room.getId()) ? soldByRoom.get(room.getId())[0] : 0;
            if (units == 0 && sold == 0) continue;
            BigDecimal revenue = revenueByRoom.getOrDefault(room.getId(), BigDecimal.ZERO);
            rows.add(occupancyRow(room.getId(), room.getName(), units, nights, sold, revenue));
            totalUnits += units;
            totalSold += sold;
            totalRevenue = totalRevenue.add(revenue);
        }
        rows.sort(Comparator.comparing(r -> r.getRoomName().orElse(""), String.CASE_INSENSITIVE_ORDER));

        return new OccupancyReport(
                range.from().toString(), range.to().toString(), Math.toIntExact(nights), rows,
                occupancyRow(null, null, totalUnits, nights, totalSold, totalRevenue));
    }

    /** One segment's share of a report range: the nights of it inside the range and their prorated price. */
    private record SegmentInRange(BookingSegmentEntity segment, long nights, BigDecimal revenue) {}

    /**
     * The room-nights population shared by occupancy, top production and market segment: every
     * segment of a non-{@code CANCELLED} booking, clipped to the range's nights, its
     * {@code totalPrice} prorated to them unrounded. One definition, so the three reports always
     * sum to the same totals for the same range.
     */
    private List<SegmentInRange> segmentsInRange(ReportDateRange range) {
        LocalDate rangeEnd = range.to().plusDays(1); // exclusive, like a checkOut
        List<SegmentInRange> slices = new ArrayList<>();
        for (BookingSegmentEntity segment : segmentRepository.findByBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                BookingStatus.CANCELLED, rangeEnd, range.from())) {
            long segmentNights = ChronoUnit.DAYS.between(segment.getCheckIn(), segment.getCheckOut());
            LocalDate clippedStart = max(segment.getCheckIn(), range.from());
            LocalDate clippedEnd = min(segment.getCheckOut(), rangeEnd);
            long inRange = ChronoUnit.DAYS.between(clippedStart, clippedEnd);
            if (segmentNights <= 0 || inRange <= 0) continue;

            BigDecimal prorated = inRange == segmentNights
                    ? segment.getTotalPrice()
                    : segment.getTotalPrice().multiply(BigDecimal.valueOf(inRange)).divide(BigDecimal.valueOf(segmentNights), MC);
            slices.add(new SegmentInRange(segment, inRange, prorated));
        }
        return slices;
    }

    private static OccupancyReportRow occupancyRow(String roomId, String roomName, long units, long nights, long sold, BigDecimal revenue) {
        long available = units * nights;
        BigDecimal availableDec = BigDecimal.valueOf(available);
        BigDecimal soldDec = BigDecimal.valueOf(sold);
        return new OccupancyReportRow(
                roomId,
                roomName,
                Math.toIntExact(units),
                Math.toIntExact(available),
                Math.toIntExact(sold),
                available == 0 ? null : money(soldDec.multiply(HUNDRED).divide(availableDec, MC)),
                money(revenue),
                sold == 0 ? null : money(revenue.divide(soldDec, MC)),
                available == 0 ? null : money(revenue.divide(availableDec, MC)));
    }

    // --- GET /reports/pos-sales-mix -----------------------------------------------------------

    /**
     * Driven by {@code Payment.createdAt}, the settlement instant, bucketed into Bangkok days by
     * {@link ReportDateRange} exactly as {@link RevenueExportService} does. {@code Payment} is
     * unique per order ({@code Payment_orderId_key}, V14), so each order is settled on exactly
     * one date - there is no "first vs. last payment" to choose between.
     */
    @Transactional(readOnly = true)
    public PosSalesMixReport posSalesMix(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        PosLines pos = posLinesInRange(range);
        List<OrderItemEntity> lines = pos.lines();
        Map<String, MenuItemEntity> menuItems = pos.menuItems();

        Map<String, Tally> byItem = new LinkedHashMap<>();
        for (OrderItemEntity line : lines) {
            byItem.computeIfAbsent(line.getMenuItemId(), k -> new Tally()).add(line.getQuantity(), line.getUnitPrice());
        }

        Map<String, Tally> byCategory = new LinkedHashMap<>();
        Map<MenuDepartment, Tally> byDepartment = new LinkedHashMap<>();
        Tally total = new Tally();
        List<PosSalesMixItem> items = new ArrayList<>();
        for (Map.Entry<String, Tally> entry : byItem.entrySet()) {
            MenuItemEntity menuItem = menuItems.get(entry.getKey()); // FK on OrderItem.menuItemId - always present
            Tally tally = entry.getValue();
            items.add(new PosSalesMixItem(
                    entry.getKey(), menuItem.getName(), menuItem.getCategory(), menuItem.getDepartment(), tally.quantityInt(), money(tally.revenue)));
            byCategory.computeIfAbsent(menuItem.getCategory(), k -> new Tally()).add(tally);
            byDepartment.computeIfAbsent(menuItem.getDepartment(), k -> new Tally()).add(tally);
            total.add(tally);
        }

        List<PosSalesMixCategory> categories = byCategory.entrySet().stream()
                .sorted(byRevenueDesc())
                .map(e -> new PosSalesMixCategory(e.getKey(), e.getValue().quantityInt(), money(e.getValue().revenue)))
                .toList();
        List<PosSalesMixDepartment> departments = byDepartment.entrySet().stream()
                .sorted(byRevenueDesc())
                .map(e -> new PosSalesMixDepartment(e.getKey(), e.getValue().quantityInt(), money(e.getValue().revenue)))
                .toList();
        items.sort(Comparator.comparing((PosSalesMixItem i) -> new BigDecimal(i.getRevenue())).reversed()
                .thenComparing(PosSalesMixItem::getName, String.CASE_INSENSITIVE_ORDER));

        return new PosSalesMixReport(
                range.from().toString(), range.to().toString(), total.quantityInt(), money(total.revenue), items, categories, departments);
    }

    /** The order lines of a range's POS population, with the menu item each one refers to. */
    private record PosLines(List<OrderItemEntity> lines, Map<String, MenuItemEntity> menuItems) {}

    /**
     * The POS population shared by the sales mix and the revenue statistic: every line of every
     * {@code PAID} order whose {@code Payment} falls inside the range's Bangkok days. One
     * definition, so the revenue statistic's FNB/SPA always sum to the sales mix's total.
     */
    private PosLines posLinesInRange(ReportDateRange range) {
        List<String> orderIds = paymentRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(range.startUtc(clock.getZone()), range.endUtcExclusive(clock.getZone()))
                .stream().map(PaymentEntity::getOrderId).distinct().toList();
        List<String> paidOrderIds = orderRepository.findAllById(orderIds).stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(OrderEntity::getId)
                .toList();
        List<OrderItemEntity> lines = paidOrderIds.isEmpty() ? List.of() : orderItemRepository.findByOrderIdIn(paidOrderIds);
        Map<String, MenuItemEntity> menuItems = byId(
                menuItemRepository.findAllById(lines.stream().map(OrderItemEntity::getMenuItemId).distinct().toList()), MenuItemEntity::getId);
        return new PosLines(lines, menuItems);
    }

    /** Revenue descending, then key ascending so equal-revenue rows come back in a stable order. */
    private static <K extends Comparable<? super K>> Comparator<Map.Entry<K, Tally>> byRevenueDesc() {
        return Comparator.comparing((Map.Entry<K, Tally> e) -> e.getValue().revenue).reversed()
                .thenComparing(Map.Entry::getKey);
    }

    private static final class Tally {
        long quantity;
        BigDecimal revenue = BigDecimal.ZERO;

        void add(int lineQuantity, BigDecimal unitPrice) {
            quantity += lineQuantity;
            revenue = revenue.add(unitPrice.multiply(BigDecimal.valueOf(lineQuantity)));
        }

        void add(Tally other) {
            quantity += other.quantity;
            revenue = revenue.add(other.revenue);
        }

        int quantityInt() {
            return Math.toIntExact(quantity);
        }
    }

    // --- GET /reports/top-production and /reports/market-segment -------------------------------

    /**
     * The occupancy population ({@link #segmentsInRange}) grouped by producer: a comp or
     * house-use booking under its purpose, whatever its channel; every other booking under its
     * own channel. Only producers with room-nights in the range get a row.
     */
    @Transactional(readOnly = true)
    public TopProductionReport topProduction(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        List<SegmentInRange> slices = segmentsInRange(range);
        Map<String, BookingEntity> bookings = bookingsOf(slices);

        Map<String, RoomNightTally> byProducer = new HashMap<>();
        RoomNightTally total = new RoomNightTally();
        for (SegmentInRange slice : slices) {
            BookingEntity booking = bookings.get(slice.segment().getBookingId());
            byProducer.computeIfAbsent(producerOf(booking), k -> new RoomNightTally()).add(slice, booking);
            total.add(slice, booking);
        }

        List<TopProductionRow> rows = byProducer.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, RoomNightTally> e) -> e.getValue().nights).reversed()
                        .thenComparing((Map.Entry<String, RoomNightTally> e) -> e.getValue().revenue, Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(e -> topProductionRow(e.getKey(), producerLabel(e.getKey()), e.getValue(), total))
                .toList();
        return new TopProductionReport(range.from().toString(), range.to().toString(), rows, topProductionRow(null, null, total, total));
    }

    private static TopProductionRow topProductionRow(String producer, String label, RoomNightTally tally, RoomNightTally total) {
        return new TopProductionRow(
                producer,
                label,
                tally.nightsInt(),
                percent(BigDecimal.valueOf(tally.nights), BigDecimal.valueOf(total.nights)),
                money(tally.revenue),
                percent(tally.revenue, total.revenue),
                tally.nights == 0 ? null : money(tally.revenue.divide(BigDecimal.valueOf(tally.nights), MC)));
    }

    /** Top production's grouping key: a {@link BookingChannel} value, or the purpose for a comp/house-use stay. */
    private static String producerOf(BookingEntity booking) {
        return switch (booking.getPurpose()) {
            case COMPLIMENTARY, HOUSE_USE -> booking.getPurpose().getValue();
            case STANDARD -> booking.getChannel().getValue();
        };
    }

    private static String producerLabel(String producer) {
        if (producer.equals(BookingPurpose.COMPLIMENTARY.getValue())) return "Complimentary";
        if (producer.equals(BookingPurpose.HOUSE_USE.getValue())) return "House Use";
        return switch (BookingChannel.fromValue(producer)) {
            case DIRECT -> "Direct";
            case PHONE -> "Phone";
            case WALK_IN -> "Walk-in";
            case BOOKING_COM -> "Booking.com";
            case AIRBNB -> "Airbnb";
            case AGODA -> "Agoda";
            case EXPEDIA -> "Expedia";
            case OTHER -> "Other";
        };
    }

    /**
     * Same population as {@link #topProduction}, rolled up one level. All five segments are
     * always returned so the report reads like the legacy Z360 sheet, zero rows included.
     * {@code guests} counts each booking's {@code adults + children} once, however many of its
     * nights or segments fall in the range - a head count of parties, not guest-nights.
     */
    @Transactional(readOnly = true)
    public MarketSegmentReport marketSegment(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        List<SegmentInRange> slices = segmentsInRange(range);
        Map<String, BookingEntity> bookings = bookingsOf(slices);

        Map<MarketSegment, RoomNightTally> bySegment = new EnumMap<>(MarketSegment.class);
        for (MarketSegment segment : MarketSegment.values()) {
            bySegment.put(segment, new RoomNightTally());
        }
        RoomNightTally total = new RoomNightTally();
        for (SegmentInRange slice : slices) {
            BookingEntity booking = bookings.get(slice.segment().getBookingId());
            bySegment.get(marketSegmentOf(booking)).add(slice, booking);
            total.add(slice, booking);
        }

        List<MarketSegmentRow> rows = bySegment.entrySet().stream()
                .map(e -> marketSegmentRow(e.getKey(), e.getValue(), total))
                .toList();
        return new MarketSegmentReport(range.from().toString(), range.to().toString(), rows, marketSegmentRow(null, total, total));
    }

    private static MarketSegmentRow marketSegmentRow(MarketSegment segment, RoomNightTally tally, RoomNightTally total) {
        return new MarketSegmentRow(
                segment,
                tally.nightsInt(),
                percent(BigDecimal.valueOf(tally.nights), BigDecimal.valueOf(total.nights)),
                Math.toIntExact(tally.guests),
                percent(BigDecimal.valueOf(tally.guests), BigDecimal.valueOf(total.guests)),
                money(tally.revenue),
                percent(tally.revenue, total.revenue),
                tally.nights == 0 ? null : money(tally.revenue.divide(BigDecimal.valueOf(tally.nights), MC)));
    }

    private static MarketSegment marketSegmentOf(BookingEntity booking) {
        return switch (booking.getPurpose()) {
            case COMPLIMENTARY -> MarketSegment.COM;
            case HOUSE_USE -> MarketSegment.HFO;
            case STANDARD -> switch (booking.getChannel()) {
                case BOOKING_COM, AIRBNB, AGODA, EXPEDIA, OTHER -> MarketSegment.OTA;
                case WALK_IN -> MarketSegment.WLK;
                case DIRECT, PHONE -> MarketSegment.DIR;
            };
        };
    }

    /** The slices' bookings, loaded in one query rather than through each segment's lazy {@code booking}. */
    private Map<String, BookingEntity> bookingsOf(List<SegmentInRange> slices) {
        List<String> ids = slices.stream().map(s -> s.segment().getBookingId()).distinct().toList();
        return ids.isEmpty() ? Map.of() : byId(bookingRepository.findAllById(ids), BookingEntity::getId);
    }

    /** Room-nights, prorated revenue and party head count; a booking's guests are added once, on its first slice. */
    private static final class RoomNightTally {
        long nights;
        BigDecimal revenue = BigDecimal.ZERO;
        long guests;
        final Set<String> bookingIds = new HashSet<>();

        void add(SegmentInRange slice, BookingEntity booking) {
            nights += slice.nights();
            revenue = revenue.add(slice.revenue());
            if (bookingIds.add(booking.getId())) {
                guests += booking.getAdults() + booking.getChildren();
            }
        }

        int nightsInt() {
            return Math.toIntExact(nights);
        }
    }

    // --- GET /reports/in-house ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public InHouseReport inHouse(String date) {
        LocalDate night = ReportDateRange.parseDateOrToday(date, clock);
        ReportDateRange range = ReportDateRange.night(night);
        List<SegmentInRange> slices = segmentsInRange(range);
        List<InHouseStay> stays = new ArrayList<>(inHouseStays(range, slices, bookingsOf(slices)));
        // A guest still checked in past checkOut is in the house tonight, and was on every night
        // since (OverstayRule) - no agreed segment covers those nights, so they're added here as
        // a zero-revenue slice of the last segment's room. The in-house list only: the occupancy
        // and manager reports stay on agreed, priced room-nights.
        LocalDate today = overstayRule.today();
        overstayRule.current().stream()
                .filter(o -> o.covers(night))
                .forEach(o -> stays.add(new InHouseStay(new SegmentInRange(o.lastSegment(), 1, BigDecimal.ZERO), o.booking())));

        Map<String, String> roomNames = roomRepository.findAllById(stays.stream().map(s -> s.segment().getRoomId()).distinct().toList())
                .stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));
        Map<String, String> unitLabels = roomUnitRepository.findAllById(
                        stays.stream().map(s -> s.segment().getRoomUnitId()).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getLabel));

        List<InHouseRow> rows = stays.stream()
                .map(s -> new InHouseRow(
                        s.booking().getId(),
                        roomNames.get(s.segment().getRoomId()),
                        unitLabels.get(s.segment().getRoomUnitId()),
                        s.booking().getAdults(),
                        s.booking().getChildren(),
                        s.booking().getGuestName(),
                        marketSegmentOf(s.booking()),
                        s.booking().getCheckIn().toString(),
                        s.booking().getCheckOut().toString())
                        .overdueDays(OverstayRule.overdueDays(s.booking(), today)))
                .sorted(Comparator.comparing((InHouseRow r) -> r.getRoomUnitLabel().orElse(null), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(InHouseRow::getRoomName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        InHouseTally total = InHouseTally.of(stays);
        return new InHouseReport(night.toString(), rows, new InHouseTotal(total.roomsInt(), total.adultsInt(), total.childrenInt()));
    }

    /** One in-house room for a night: that night's slice of a segment, and its booking. */
    private record InHouseStay(SegmentInRange slice, BookingEntity booking) {
        BookingSegmentEntity segment() {
            return slice.segment();
        }
    }

    /**
     * The one-night occupancy population narrowed to guests physically present that night.
     * {@code BookingWriter#assertContinuity} keeps a booking's segments from overlapping, so each
     * booking contributes at most one slice - one room - to a single night: a booking's
     * {@code adults}/{@code children} never need splitting across rooms.
     */
    private List<InHouseStay> inHouseStays(ReportDateRange night, List<SegmentInRange> slices, Map<String, BookingEntity> bookings) {
        LocalDateTime nextMorningUtc = night.endUtcExclusive(clock.getZone());
        return slices.stream()
                .map(slice -> new InHouseStay(slice, bookings.get(slice.segment().getBookingId())))
                .filter(stay -> wasInHouse(stay.booking(), nextMorningUtc))
                .toList();
    }

    /**
     * {@code CHECKED_IN}, or {@code CHECKED_OUT} on a later day than the night (only possible
     * for a past night) - so a past night's list still shows who was here then. {@code
     * checkedOutAt} is the shared clock's instant as UTC wall-clock ({@code
     * BookingOccupancyService#nowUtc}), compared like every {@code @CreationTimestamp} in {@link
     * ReportDateRange}; check-out is its only writer and always sets it.
     */
    private static boolean wasInHouse(BookingEntity booking, LocalDateTime nextMorningUtc) {
        return switch (booking.getOccupancyStatus()) {
            case CHECKED_IN -> true;
            case CHECKED_OUT -> booking.getCheckedOutAt() != null && !booking.getCheckedOutAt().isBefore(nextMorningUtc);
            case EXPECTED, NO_SHOW -> false;
        };
    }

    /** The in-house list's totals - the manager report's guest statistic reads the same tally. */
    private static final class InHouseTally {
        long rooms;
        long adults;
        long children;
        long complimentaryGuests;
        long houseUseGuests;
        long stayNights;
        BigDecimal revenue = BigDecimal.ZERO;

        static InHouseTally of(List<InHouseStay> stays) {
            InHouseTally tally = new InHouseTally();
            for (InHouseStay stay : stays) {
                BookingEntity booking = stay.booking();
                long guests = booking.getAdults() + booking.getChildren();
                tally.rooms++;
                tally.adults += booking.getAdults();
                tally.children += booking.getChildren();
                if (booking.getPurpose() == BookingPurpose.COMPLIMENTARY) tally.complimentaryGuests += guests;
                if (booking.getPurpose() == BookingPurpose.HOUSE_USE) tally.houseUseGuests += guests;
                tally.stayNights += ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
                tally.revenue = tally.revenue.add(stay.slice().revenue());
            }
            return tally;
        }

        long guests() {
            return adults + children;
        }

        int roomsInt() {
            return Math.toIntExact(rooms);
        }

        int adultsInt() {
            return Math.toIntExact(adults);
        }

        int childrenInt() {
            return Math.toIntExact(children);
        }
    }

    // --- GET /reports/manager -----------------------------------------------------------------

    /** The same {@link #managerDay} twice: the night, and the same calendar date a year earlier. */
    @Transactional(readOnly = true)
    public ManagerReport manager(String date) {
        LocalDate night = ReportDateRange.parseDateOrToday(date, clock);
        LocalDate lastYear = night.minusYears(1);
        Set<String> activeUnitIds = activeUnitsByRoom().keySet();
        return new ManagerReport(night.toString(), lastYear.toString(), managerDay(night, activeUnitIds), managerDay(lastYear, activeUnitIds));
    }

    /**
     * Occupied rooms and revenue come from {@link #segmentsInRange} - the occupancy report's own
     * population, so {@code occupied} is its {@code roomNightsSold} for the night by
     * construction. The guest statistic is {@link #inHouseStays} over those same slices.
     */
    private ManagerReportDay managerDay(LocalDate night, Set<String> activeUnitIds) {
        ReportDateRange range = ReportDateRange.night(night);
        List<SegmentInRange> slices = segmentsInRange(range);
        Map<String, BookingEntity> bookings = bookingsOf(slices);

        long occupied = 0, complimentary = 0, houseUse = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (SegmentInRange slice : slices) {
            occupied += slice.nights();
            revenue = revenue.add(slice.revenue());
            switch (bookings.get(slice.segment().getBookingId()).getPurpose()) {
                case COMPLIMENTARY -> complimentary += slice.nights();
                case HOUSE_USE -> houseUse += slice.nights();
                case STANDARD -> {}
            }
        }
        long outOfOrder = outOfOrderUnits(night, activeUnitIds);
        long availableForSale = activeUnitIds.size() - outOfOrder;
        BigDecimal occupiedDec = BigDecimal.valueOf(occupied);
        BigDecimal availableDec = BigDecimal.valueOf(availableForSale);
        ManagerRoomStatistic rooms = new ManagerRoomStatistic(
                activeUnitIds.size(),
                Math.toIntExact(outOfOrder),
                Math.toIntExact(availableForSale),
                Math.toIntExact(occupied),
                Math.toIntExact(complimentary),
                Math.toIntExact(houseUse),
                Math.toIntExact(occupied - complimentary - houseUse),
                percent(occupiedDec, availableDec),
                ratio(revenue, occupiedDec),
                ratio(revenue, availableDec));

        InHouseTally inHouse = InHouseTally.of(inHouseStays(range, slices, bookings));
        BigDecimal guests = BigDecimal.valueOf(inHouse.guests());
        ManagerGuestStatistic guestStatistic = new ManagerGuestStatistic(
                inHouse.adultsInt(),
                inHouse.childrenInt(),
                Math.toIntExact(inHouse.guests()),
                ratio(guests, BigDecimal.valueOf(inHouse.rooms)),
                ratio(inHouse.revenue, guests),
                ratio(BigDecimal.valueOf(inHouse.stayNights), BigDecimal.valueOf(inHouse.rooms)),
                Math.toIntExact(inHouse.complimentaryGuests),
                Math.toIntExact(inHouse.houseUseGuests));

        List<BookingEntity> arrivals = bookingRepository.findByStatusNotAndCheckInIs(BookingStatus.CANCELLED, night);
        ManagerAccountCount accounts = new ManagerAccountCount(
                arrivals.size(),
                bookingRepository.findByStatusNotAndCheckOut(BookingStatus.CANCELLED, night).size(),
                Math.toIntExact(bookingRepository.countByStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                        BookingStatus.CANCELLED, range.startUtc(clock.getZone()), range.endUtcExclusive(clock.getZone()))),
                Math.toIntExact(bookingRepository.countByOccupancyStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                        OccupancyStatus.NO_SHOW, range.startUtc(clock.getZone()), range.endUtcExclusive(clock.getZone()))),
                Math.toIntExact(arrivals.stream().filter(b -> b.getChannel() == BookingChannel.WALK_IN).count()));

        return new ManagerReportDay(
                rooms, guestStatistic, accounts, new ManagerRevenue(money(revenue), ratio(revenue, guests)), forecast(night.plusDays(1), activeUnitIds));
    }

    private ManagerForecast forecast(LocalDate night, Set<String> activeUnitIds) {
        long occupied = segmentsInRange(ReportDateRange.night(night)).stream().mapToLong(SegmentInRange::nights).sum();
        long availableForSale = activeUnitIds.size() - outOfOrderUnits(night, activeUnitIds);
        return new ManagerForecast(
                night.toString(),
                bookingRepository.findByStatusNotAndCheckInIs(BookingStatus.CANCELLED, night).size(),
                bookingRepository.findByStatusNotAndCheckOut(BookingStatus.CANCELLED, night).size(),
                Math.toIntExact(occupied),
                Math.toIntExact(availableForSale),
                percent(BigDecimal.valueOf(occupied), BigDecimal.valueOf(availableForSale)));
    }

    /** Active units with a block covering the night; a unit under two overlapping blocks counts once. */
    private long outOfOrderUnits(LocalDate night, Set<String> activeUnitIds) {
        return outOfOrderUnitIds(night, activeUnitIds).size();
    }

    private Set<String> outOfOrderUnitIds(LocalDate night, Set<String> activeUnitIds) {
        return blockRepository.findByFromDateLessThanEqualAndToDateGreaterThanEqual(night, night).stream()
                .map(RoomUnitBlockEntity::getRoomUnitId)
                .filter(activeUnitIds::contains)
                .collect(Collectors.toSet());
    }

    /** Physical units active today, unit id to room type id - the manager report's {@code totalRooms}. */
    private Map<String, String> activeUnitsByRoom() {
        return roomUnitRepository.findAll().stream()
                .filter(RoomUnitEntity::isActive)
                .collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getRoomId));
    }

    // --- GET /reports/forecast ----------------------------------------------------------------

    /**
     * The manager report's per-night figures, one date at a time and split by room type:
     * occupied from {@link #segmentsInRange} over that one night, arrivals/departures from the
     * manager report's own two booking queries, out-of-order from {@link #outOfOrderUnitIds}. A
     * few queries per date - the per-night reuse is what keeps every date's {@code total} equal to
     * the occupancy and manager reports for that night by construction.
     */
    @Transactional(readOnly = true)
    public ForecastReport forecast(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        Map<String, String> roomOfUnit = activeUnitsByRoom();
        Map<String, Long> unitsByRoom = roomOfUnit.values().stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, String> roomNames = roomRepository.findAll().stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));

        List<ForecastDay> days = new ArrayList<>();
        for (LocalDate date = range.from(); !date.isAfter(range.to()); date = date.plusDays(1)) {
            days.add(forecastDay(date, roomOfUnit, unitsByRoom, roomNames));
        }
        return new ForecastReport(range.from().toString(), range.to().toString(), days);
    }

    private ForecastDay forecastDay(LocalDate date, Map<String, String> roomOfUnit, Map<String, Long> unitsByRoom, Map<String, String> roomNames) {
        Map<String, ForecastTally> byRoom = new HashMap<>();
        unitsByRoom.forEach((roomId, units) -> byRoom.computeIfAbsent(roomId, k -> new ForecastTally()).totalRooms = units);

        List<SegmentInRange> slices = segmentsInRange(ReportDateRange.night(date));
        Map<String, String> roomOfArrivalNight = new HashMap<>();
        for (SegmentInRange slice : slices) {
            byRoom.computeIfAbsent(slice.segment().getRoomId(), k -> new ForecastTally()).occupied += slice.nights();
            if (slice.segment().getCheckIn().equals(date)) {
                roomOfArrivalNight.put(slice.segment().getBookingId(), slice.segment().getRoomId());
            }
        }
        // An arrival's own first night is always one of tonight's slices (checkOut > checkIn), so
        // falling back to the booking's roomId - its last segment's room - is only a safety net.
        for (BookingEntity arrival : bookingRepository.findByStatusNotAndCheckInIs(BookingStatus.CANCELLED, date)) {
            byRoom.computeIfAbsent(roomOfArrivalNight.getOrDefault(arrival.getId(), arrival.getRoomId()), k -> new ForecastTally()).arrivals++;
        }
        for (BookingEntity departure : bookingRepository.findByStatusNotAndCheckOut(BookingStatus.CANCELLED, date)) {
            byRoom.computeIfAbsent(departure.getRoomId(), k -> new ForecastTally()).departures++;
        }
        for (String unitId : outOfOrderUnitIds(date, roomOfUnit.keySet())) {
            byRoom.get(roomOfUnit.get(unitId)).outOfOrder++;
        }

        ForecastTally total = new ForecastTally();
        byRoom.values().forEach(total::add);
        List<ForecastRow> rows = byRoom.entrySet().stream()
                .map(e -> e.getValue().row(e.getKey(), roomNames.get(e.getKey())))
                .sorted(Comparator.comparing(r -> r.getRoomName().orElse(""), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ForecastDay(date.toString(), rows, total.row(null, null));
    }

    /** One room type's (or the property's) forecast counts for a date; vacant and occupancy % are derived. */
    private static final class ForecastTally {
        long arrivals;
        long departures;
        long totalRooms;
        long outOfOrder;
        long occupied;

        void add(ForecastTally other) {
            arrivals += other.arrivals;
            departures += other.departures;
            totalRooms += other.totalRooms;
            outOfOrder += other.outOfOrder;
            occupied += other.occupied;
        }

        ForecastRow row(String roomId, String roomName) {
            return new ForecastRow(
                    roomId,
                    roomName,
                    Math.toIntExact(arrivals),
                    Math.toIntExact(departures),
                    Math.toIntExact(totalRooms),
                    Math.toIntExact(outOfOrder),
                    Math.toIntExact(occupied),
                    Math.toIntExact(totalRooms - occupied - outOfOrder),
                    percent(BigDecimal.valueOf(occupied), BigDecimal.valueOf(totalRooms - outOfOrder)));
        }
    }

    // --- GET /reports/guest-ltv ---------------------------------------------------------------

    /**
     * Ranked and cut to {@code limit} in SQL ({@link BookingRepository#sumByGuest}); only the
     * returned guests' bookings are then loaded, for nights and room charges.
     *
     * <p>{@code Booking.totalPrice} is room price only - {@code BookingWriter#syncBookingFromSegments}
     * sets it to the sum of the segments' totals, and nothing else ever adds to it. POS spend
     * charged to the room lives in {@code ROOM_CHARGE} {@link PaymentEntity} rows keyed by
     * {@code bookingId} (see {@link BookingService#computeFolio}), so it is reported beside the
     * room figure as {@code roomChargesTotal}, never folded into the ranking.
     */
    @Transactional(readOnly = true)
    public GuestLtvReport guestLtv(Integer limit) {
        int pageSize = limit != null ? limit : 50;
        List<BookingRepository.GuestBookingTotals> totals = bookingRepository.sumByGuest(BookingStatus.CANCELLED, PageRequest.of(0, pageSize));
        if (totals.isEmpty()) {
            return new GuestLtvReport(List.of());
        }

        List<String> guestIds = totals.stream().map(BookingRepository.GuestBookingTotals::getGuestId).toList();
        Map<String, GuestEntity> guests = byId(guestRepository.findAllById(guestIds), GuestEntity::getId);
        List<BookingEntity> bookings = bookingRepository.findByGuestIdInAndStatusNot(guestIds, BookingStatus.CANCELLED);
        Map<String, String> guestByBooking = bookings.stream().collect(Collectors.toMap(BookingEntity::getId, BookingEntity::getGuestId));
        List<String> bookingIds = List.copyOf(guestByBooking.keySet());

        Map<String, Long> nightsByGuest = new HashMap<>();
        for (BookingSegmentEntity segment : segmentRepository.findByBookingIdIn(bookingIds)) {
            nightsByGuest.merge(guestByBooking.get(segment.getBookingId()), ChronoUnit.DAYS.between(segment.getCheckIn(), segment.getCheckOut()), Long::sum);
        }
        Map<String, BigDecimal> chargesByGuest = new HashMap<>();
        for (PaymentEntity payment : paymentRepository.findByBookingIdInAndMethod(bookingIds, PaymentMethod.ROOM_CHARGE)) {
            chargesByGuest.merge(guestByBooking.get(payment.getBookingId()), payment.getAmount(), BigDecimal::add);
        }

        List<GuestLtvRow> rows = totals.stream()
                .map(t -> {
                    GuestEntity guest = guests.get(t.getGuestId()); // FK on Booking.guestId - always present
                    return new GuestLtvRow(
                            t.getGuestId(),
                            guest.getName(),
                            guest.getEmail(),
                            Math.toIntExact(t.getBookingCount()),
                            Math.toIntExact(nightsByGuest.getOrDefault(t.getGuestId(), 0L)),
                            money(t.getRoomRevenue()),
                            money(chargesByGuest.getOrDefault(t.getGuestId(), BigDecimal.ZERO)),
                            t.getFirstCheckIn().toString(),
                            t.getLastCheckIn().toString());
                })
                .toList();
        return new GuestLtvReport(rows);
    }

    // --- GET /reports/revenue-statistic ---------------------------------------------------------

    /**
     * ROOM is {@link #segmentsInRange} (occupancy's own population), FNB/SPA are
     * {@link #posLinesInRange} (the sales mix's) split by menu department - the classification
     * lives here, at report time, not on any stored row. Every price in this system is
     * VAT-inclusive (nothing ever adds tax on top of one), so VAT is extracted from gross, never
     * added to it. Each code's gross is rounded first, exactly as its source report renders it;
     * VAT is computed from that rounded gross, and net is the remainder, so net + vat = gross to
     * the cent and the total row is a plain sum of the rows.
     */
    @Transactional(readOnly = true)
    public RevenueStatisticReport revenueStatistic(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        BigDecimal vatRate = vatSettingsService.currentRate();

        Map<RevenueCode, BigDecimal> gross = new EnumMap<>(RevenueCode.class);
        for (RevenueCode code : RevenueCode.values()) {
            gross.put(code, BigDecimal.ZERO);
        }
        for (SegmentInRange slice : segmentsInRange(range)) {
            gross.merge(RevenueCode.ROOM, slice.revenue(), BigDecimal::add);
        }
        PosLines pos = posLinesInRange(range);
        for (OrderItemEntity line : pos.lines()) {
            MenuItemEntity menuItem = pos.menuItems().get(line.getMenuItemId()); // FK on OrderItem.menuItemId - always present
            gross.merge(RevenueClassification.of(menuItem.getDepartment()), line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())), BigDecimal::add);
        }

        List<RevenueStatisticRow> rows = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO, totalVat = BigDecimal.ZERO;
        for (Map.Entry<RevenueCode, BigDecimal> entry : gross.entrySet()) {
            BigDecimal rowGross = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal rowVat = RevenueClassification.vatInside(rowGross, vatRate);
            rows.add(revenueStatisticRow(entry.getKey(), rowGross, rowVat));
            totalGross = totalGross.add(rowGross);
            totalVat = totalVat.add(rowVat);
        }
        return new RevenueStatisticReport(
                range.from().toString(), range.to().toString(), money(vatRate), rows, revenueStatisticRow(null, totalGross, totalVat));
    }

    private static RevenueStatisticRow revenueStatisticRow(RevenueCode code, BigDecimal gross, BigDecimal vat) {
        return new RevenueStatisticRow(code, money(gross), money(vat), money(gross.subtract(vat)));
    }

    // --- Shared -------------------------------------------------------------------------------

    private static String money(BigDecimal value) {
        return PriceFormat.asDecimalString(value.setScale(2, RoundingMode.HALF_UP));
    }

    /** part / whole × 100, two decimals; null when whole is zero. */
    private static String percent(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? null : money(part.multiply(HUNDRED).divide(whole, MC));
    }

    /** part / whole, two decimals; null when whole is zero. */
    private static String ratio(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? null : money(part.divide(whole, MC));
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static <T> Map<String, T> byId(List<T> rows, Function<T, String> id) {
        return rows.stream().collect(Collectors.toMap(id, r -> r));
    }
}
