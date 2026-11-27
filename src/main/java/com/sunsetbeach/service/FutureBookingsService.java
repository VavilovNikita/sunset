package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.mapper.DateText;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.FutureBookingRow;
import com.sunsetbeach.model.FutureBookingsReport;
import com.sunsetbeach.model.FutureBookingsStatusSummary;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /reports/future-bookings}: the bookings arriving in a date range, by status (Tentative = {@code NEW},
 * Confirmed, Room paid). Read-only. The one balance figure is {@link BookingFolioCalculator}'s, computed for the whole
 * list in two queries; money in the summary is added here, never by a client.
 */
@Service
public class FutureBookingsService {

    static final int DEFAULT_DAYS = 90;
    static final int MAX_DAYS = 366;
    /** Every status the report can show, in the order the summary lists them. */
    static final List<BookingStatus> REPORTABLE = List.of(BookingStatus.NEW, BookingStatus.CONFIRMED, BookingStatus.PAID);

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingFolioCalculator folioCalculator;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public FutureBookingsService(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            BookingFolioCalculator folioCalculator,
            AuditLogService auditLogService,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.folioCalculator = folioCalculator;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FutureBookingsReport report(String from, String to, List<BookingStatus> statuses) {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = parse("from", from, today);
        LocalDate toDate = to != null ? parse("to", to, null) : fromDate.plusDays(DEFAULT_DAYS);
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("from must be on or before to");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) + 1 > MAX_DAYS) {
            throw new BadRequestException("Range too large (max " + MAX_DAYS + " days)");
        }
        Set<BookingStatus> wanted = resolveStatuses(statuses);
        List<BookingStatus> ordered = REPORTABLE.stream().filter(wanted::contains).toList();

        List<BookingEntity> bookings = bookingRepository.findArrivals(fromDate, toDate, ordered, OccupancyStatus.NO_SHOW);
        Map<String, BookingSegmentEntity> firstSegments = firstSegments(bookings);
        Map<String, String> roomNames = roomRepository
                .findAllById(firstSegments.values().stream().map(BookingSegmentEntity::getRoomId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));
        Map<String, String> unitLabels = roomUnitRepository
                .findAllById(firstSegments.values().stream().map(BookingSegmentEntity::getRoomUnitId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getLabel));
        Map<String, BookingFolioCalculator.FolioBreakdown> folios = folioCalculator.breakdowns(bookings);

        List<FutureBookingRow> rows = new ArrayList<>();
        Map<BookingStatus, int[]> counts = new EnumMap<>(BookingStatus.class);
        Map<BookingStatus, BigDecimal> totals = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : ordered) {
            counts.put(status, new int[2]);
            totals.put(status, BigDecimal.ZERO);
        }
        for (BookingEntity b : bookings) {
            BookingSegmentEntity first = firstSegments.get(b.getId());
            int nights = (int) ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            rows.add(new FutureBookingRow(
                    b.getId(),
                    b.getGuestName(),
                    first != null ? roomNames.getOrDefault(first.getRoomId(), "") : "",
                    first != null && first.getRoomUnitId() != null ? unitLabels.get(first.getRoomUnitId()) : null,
                    b.getCheckIn().toString(),
                    b.getCheckOut().toString(),
                    nights,
                    b.getAdults(),
                    b.getChildren(),
                    b.getChannel(),
                    b.getStatus(),
                    PriceFormat.asDecimalString(b.getTotalPrice()),
                    PriceFormat.asDecimalString(folios.get(b.getId()).balanceDue()),
                    b.getNotes()));
            counts.get(b.getStatus())[0]++;
            counts.get(b.getStatus())[1] += nights;
            totals.merge(b.getStatus(), b.getTotalPrice(), BigDecimal::add);
        }
        List<FutureBookingsStatusSummary> summary = ordered.stream()
                .map(s -> new FutureBookingsStatusSummary(s, counts.get(s)[0], counts.get(s)[1], PriceFormat.asDecimalString(totals.get(s))))
                .toList();
        return new FutureBookingsReport(fromDate.toString(), toDate.toString(), ordered, rows, summary);
    }

    @Transactional(readOnly = true)
    public Export exportCsv(String from, String to, List<BookingStatus> statuses) {
        FutureBookingsReport report = report(from, to, statuses);
        CsvBuilder csv = new CsvBuilder();
        csv.row("Booking ID", "Guest", "Room", "Room unit", "Check-in", "Check-out", "Nights", "Adults", "Children", "Channel", "Status", "Total",
                "Balance due", "Notes");
        for (FutureBookingRow r : report.getRows()) {
            csv.row(
                    r.getBookingId(),
                    r.getGuestName(),
                    r.getRoomName(),
                    r.getRoomUnitLabel().orElse(null),
                    r.getCheckIn(),
                    r.getCheckOut(),
                    String.valueOf(r.getNights()),
                    String.valueOf(r.getAdults()),
                    String.valueOf(r.getChildren()),
                    channelLabel(r.getChannel()),
                    label(r.getStatus()),
                    r.getTotalPrice(),
                    r.getBalanceDue(),
                    r.getNotes().orElse(null));
        }
        auditLogService.record(
                AuditAction.BOOKINGS_EXPORTED,
                AuditEntityType.BOOKING,
                null,
                "Exported " + report.getRows().size() + " future booking(s) arriving " + DateText.iso(report.getFrom()) + " to " + DateText.iso(report.getTo()));
        return new Export("future-bookings_" + report.getFrom() + "_" + report.getTo() + ".csv", csv.toString());
    }

    public record Export(String filename, String csv) {}

    /** What staff read for a channel - the admin screens' labels (sunset-beach {@code lib/bookingChannel.ts}), not the stored {@code PHONE}. */
    static String channelLabel(com.sunsetbeach.model.BookingChannel channel) {
        return switch (channel) {
            case DIRECT -> "Direct (website)";
            case PHONE -> "Phone";
            case WALK_IN -> "Walk-in";
            case BOOKING_COM -> "Booking.com";
            case AIRBNB -> "Airbnb";
            case AGODA -> "Agoda";
            case EXPEDIA -> "Expedia";
            case CTRIP -> "Ctrip / Trip.com";
            case HOTELBEDS -> "Hotelbeds";
            case AGENT -> "Travel agent";
            case OTHER -> "Other";
        };
    }

    /** What staff read for a status; the stored values stay {@code NEW}/{@code CONFIRMED}/{@code PAID}. */
    static String label(BookingStatus status) {
        return switch (status) {
            case NEW -> "Tentative";
            case CONFIRMED -> "Confirmed";
            case PAID -> "Room paid";
            case CANCELLED -> "Cancelled";
        };
    }

    private static Set<BookingStatus> resolveStatuses(List<BookingStatus> requested) {
        if (requested == null || requested.isEmpty()) {
            return new LinkedHashSet<>(REPORTABLE);
        }
        if (requested.contains(BookingStatus.CANCELLED)) {
            throw new BadRequestException("status can be NEW, CONFIRMED or PAID - cancelled bookings are not in this report");
        }
        return new LinkedHashSet<>(requested);
    }

    private static LocalDate parse(String field, String value, LocalDate fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be a date (YYYY-MM-DD)");
        }
    }

    /** The segment of each booking's first night - where the guest actually arrives, even if the stay is relocated later. */
    private Map<String, BookingSegmentEntity> firstSegments(List<BookingEntity> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }
        List<String> ids = bookings.stream().map(BookingEntity::getId).toList();
        Map<String, BookingSegmentEntity> first = new HashMap<>();
        segmentRepository.findByBookingIdIn(ids).stream()
                .sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn))
                .forEach(s -> first.putIfAbsent(s.getBookingId(), s));
        return first;
    }
}
