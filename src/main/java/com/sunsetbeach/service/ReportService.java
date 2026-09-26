package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestLtvReport;
import com.sunsetbeach.model.GuestLtvRow;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.OccupancyReport;
import com.sunsetbeach.model.OccupancyReportRow;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.PosSalesMixCategory;
import com.sunsetbeach.model.PosSalesMixDepartment;
import com.sunsetbeach.model.PosSalesMixItem;
import com.sunsetbeach.model.PosSalesMixReport;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The three read-only dashboard reports under {@code /reports}: occupancy/ADR/RevPAR, POS sales
 * mix, and guest lifetime value. Each operation's own openapi.yaml description is the contract;
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
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final Clock clock;

    public ReportService(
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            BookingRepository bookingRepository,
            GuestRepository guestRepository,
            Clock clock) {
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.bookingRepository = bookingRepository;
        this.guestRepository = guestRepository;
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
        LocalDate rangeEnd = range.to().plusDays(1); // exclusive, like a checkOut

        Map<String, Long> activeUnits = roomUnitRepository.countActiveGroupedByRoom().stream()
                .collect(Collectors.toMap(RoomUnitRepository.RoomActiveUnitCount::getRoomId, RoomUnitRepository.RoomActiveUnitCount::getActiveCount));

        Map<String, long[]> soldByRoom = new HashMap<>();
        Map<String, BigDecimal> revenueByRoom = new HashMap<>();
        for (BookingSegmentEntity segment : segmentRepository.findByBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                BookingStatus.CANCELLED, rangeEnd, range.from())) {
            long segmentNights = ChronoUnit.DAYS.between(segment.getCheckIn(), segment.getCheckOut());
            LocalDate clippedStart = max(segment.getCheckIn(), range.from());
            LocalDate clippedEnd = min(segment.getCheckOut(), rangeEnd);
            long inRange = ChronoUnit.DAYS.between(clippedStart, clippedEnd);
            if (segmentNights <= 0 || inRange <= 0) continue;

            soldByRoom.computeIfAbsent(segment.getRoomId(), k -> new long[1])[0] += inRange;
            BigDecimal prorated = inRange == segmentNights
                    ? segment.getTotalPrice()
                    : segment.getTotalPrice().multiply(BigDecimal.valueOf(inRange)).divide(BigDecimal.valueOf(segmentNights), MC);
            revenueByRoom.merge(segment.getRoomId(), prorated, BigDecimal::add);
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

    // --- Shared -------------------------------------------------------------------------------

    private static String money(BigDecimal value) {
        return PriceFormat.asDecimalString(value.setScale(2, RoundingMode.HALF_UP));
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
