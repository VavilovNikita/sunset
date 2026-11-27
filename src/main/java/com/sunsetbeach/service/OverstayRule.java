package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The one rule for a guest who is still {@code CHECKED_IN} after their {@code checkOut} date:
 * they are still in the house, and their room is still taken - tonight included - until someone
 * checks them out or extends the stay.
 *
 * <p>Before this existed, "who is in the house" had two definitions that disagreed exactly on
 * this guest. Screens that read {@code occupancyStatus} (the Today board, the property map) saw
 * them; screens that read the agreed dates ({@code checkIn <= night < checkOut}: availability,
 * the calendar, the in-house list, {@code GET /bookings?from&to} behind POS charge-to-room and
 * the spa's guest picker) did not, so the room was offered for sale over a real guest and the
 * guest couldn't be billed. Only the night audit's missed departures already said "N days
 * overdue". Every one of those readers now asks this class.
 *
 * <p>An overstay is never written anywhere: the booking's dates, segments and frozen nightly
 * rates stay exactly as agreed. It's derived on every read as the nights {@code [checkOut,
 * today]} in the room of the booking's last segment, so it grows by itself each day and
 * disappears the moment the guest is checked out. The ways to end one are the existing ones -
 * check-out, or {@code PATCH /bookings/{id}/schedule} to extend the stay (which prices the extra
 * nights like any other extension).
 *
 * <p>"Overdue" starts the day after {@code checkOut}: on the departure day itself the guest is
 * simply due out, and that night is still sellable to the next arrival (back-to-back turnover).
 * Same counting as the night audit's "N days overdue".
 */
@Component
public class OverstayRule {

    /**
     * A still-checked-in guest's nights past {@code checkOut}, in the room of their last segment.
     * {@code toExclusive} is tomorrow, so tonight is covered - shaped like a segment's own
     * {@code [checkIn, checkOut)} so every overlap test reads the same way.
     */
    public record Overstay(BookingEntity booking, BookingSegmentEntity lastSegment, LocalDate from, LocalDate toExclusive) {

        public String bookingId() {
            return booking.getId();
        }

        public String roomId() {
            return lastSegment.getRoomId();
        }

        public String roomUnitId() {
            return lastSegment.getRoomUnitId();
        }

        public boolean covers(LocalDate night) {
            return !night.isBefore(from) && night.isBefore(toExclusive);
        }

        /** Same half-open overlap test as a segment's {@code checkIn < otherCheckOut && checkOut > otherCheckIn}. */
        public boolean overlaps(LocalDate checkIn, LocalDate checkOut) {
            return from.isBefore(checkOut) && toExclusive.isAfter(checkIn);
        }
    }

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomNightPricing roomNightPricing;
    private final RoomRepository roomRepository;
    private final Clock clock;

    public OverstayRule(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomNightPricing roomNightPricing,
            RoomRepository roomRepository,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomNightPricing = roomNightPricing;
        this.roomRepository = roomRepository;
        this.clock = clock;
    }

    /**
     * What the room type's price list says for the nights {@code [from, toExclusive)}: the price
     * grid's manual price for a date where one is set, {@code Room.basePrice} otherwise - the same lookup {@code BookingWriter} uses to price
     * a new night ({@link RoomNightPricing}). Writes nothing: a night is charged by {@link
     * OverstayChargeService}; this prices one that has not been charged yet (tonight, before the sweep).
     */
    public BigDecimal estimatedPrice(String roomId, LocalDate from, LocalDate toExclusive) {
        if (!from.isBefore(toExclusive)) {
            return BigDecimal.ZERO;
        }
        var room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return BigDecimal.ZERO;
        }
        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
            nights.add(d);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal price : roomNightPricing.priceNights(room, nights).values()) {
            total = total.add(price);
        }
        return total;
    }

    /** The hotel's date from the shared {@link Clock} - see CLAUDE.md, "Clock and time zones". */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public static boolean isOverdue(BookingEntity booking, LocalDate today) {
        return booking.getStatus() != BookingStatus.CANCELLED
                && booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN
                && booking.getCheckOut().isBefore(today);
    }

    /** Days past {@code checkOut} for an overdue guest, {@code 0} for everyone else. */
    public static int overdueDays(BookingEntity booking, LocalDate today) {
        return isOverdue(booking, today) ? Math.toIntExact(ChronoUnit.DAYS.between(booking.getCheckOut(), today)) : 0;
    }

    /**
     * The last night a guest is actually here, for a "does this date fall within their stay"
     * question asked inclusively (the spa's out-of-stay warning): their {@code checkOut}, or
     * today while they're overdue.
     */
    public static LocalDate lastDayInHouse(BookingEntity booking, LocalDate today) {
        return isOverdue(booking, today) ? today : booking.getCheckOut();
    }

    /** Every overstay right now. Small by nature (a handful of forgotten check-outs at most), so callers filter in memory. */
    public List<Overstay> current() {
        LocalDate today = today();
        List<BookingEntity> overdue =
                bookingRepository.findByOccupancyStatusAndStatusNotAndCheckOutLessThan(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED, today);
        if (overdue.isEmpty()) {
            return List.of();
        }
        Map<String, BookingSegmentEntity> lastSegmentByBooking = segmentRepository
                .findByBookingIdIn(overdue.stream().map(BookingEntity::getId).toList()).stream()
                .collect(Collectors.toMap(
                        BookingSegmentEntity::getBookingId,
                        s -> s,
                        (a, b) -> Comparator.comparing(BookingSegmentEntity::getCheckOut).compare(a, b) >= 0 ? a : b));
        return overdue.stream()
                .map(b -> {
                    BookingSegmentEntity last = lastSegmentByBooking.get(b.getId());
                    return last == null ? null : new Overstay(b, last, b.getCheckOut(), today.plusDays(1));
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * A guest who is physically in a room right now. {@code overdueDays} is {@code 0} for one still
     * within their stay (the departure day included).
     */
    public record InHouseGuest(BookingEntity booking, int overdueDays, boolean dueOutToday) {

        public boolean overdue() {
            return overdueDays > 0;
        }
    }

    /**
     * Who is physically in a room right now, keyed by room unit: every {@code CHECKED_IN},
     * not-cancelled booking under the unit of its {@link #currentSegment current segment} - an
     * overstaying guest included, and a guest due out today who has not been checked out yet.
     * Not the same question as availability ({@link #overlapping}): a departure-day guest leaves
     * the night free to sell, but is still in the room until someone checks them out.
     */
    public Map<String, List<InHouseGuest>> inHouseByRoomUnit() {
        List<BookingEntity> inHouse =
                bookingRepository.findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED);
        if (inHouse.isEmpty()) {
            return Map.of();
        }
        LocalDate today = today();
        Map<String, List<BookingSegmentEntity>> segmentsByBooking = segmentRepository
                .findByBookingIdIn(inHouse.stream().map(BookingEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));
        Map<String, List<InHouseGuest>> byUnit = new HashMap<>();
        for (BookingEntity booking : inHouse) {
            String unitId = unitOn(segmentsByBooking.getOrDefault(booking.getId(), List.of()), booking.getRoomUnitId(), today);
            if (unitId != null) {
                byUnit.computeIfAbsent(unitId, k -> new ArrayList<>())
                        .add(new InHouseGuest(booking, overdueDays(booking, today), booking.getCheckOut().equals(today)));
            }
        }
        return byUnit;
    }

    /**
     * The other guest in {@code roomUnitId} right now, if any - the longest overdue first, since
     * that is the one nobody is expecting to find there.
     */
    public Optional<InHouseGuest> otherGuestIn(String roomUnitId, String bookingId) {
        return otherGuestIn(inHouseByRoomUnit(), roomUnitId, bookingId);
    }

    /** {@link #otherGuestIn(String, String)} over a map already read - for a list that asks about many rooms. */
    public static Optional<InHouseGuest> otherGuestIn(Map<String, List<InHouseGuest>> byUnit, String roomUnitId, String bookingId) {
        return guestInExcept(byUnit, roomUnitId, Set.of(bookingId));
    }

    /** As {@link #otherGuestIn(Map, String, String)}, leaving out several bookings (the two sides of a swap). */
    public static Optional<InHouseGuest> guestInExcept(Map<String, List<InHouseGuest>> byUnit, String roomUnitId, Set<String> bookingIds) {
        return byUnit.getOrDefault(roomUnitId, List.of()).stream()
                .filter(g -> !bookingIds.contains(g.booking().getId()))
                .max(Comparator.comparingInt(InHouseGuest::overdueDays).thenComparing(g -> g.booking().getId()));
    }

    /** The one sentence for a room another guest is still checked into; {@code whatToDo} is the caller's way out. */
    public static String occupiedMessage(String unitLabel, InHouseGuest other, String whatToDo) {
        String state = other.overdue() ? "past check-out" : other.dueOutToday() ? "due out today" : null;
        return "Room " + unitLabel + " is still occupied by " + other.booking().getGuestName() + " (checked in"
                + (state == null ? "" : ", " + state) + "). " + whatToDo;
    }

    /**
     * The physical room a stay is in on {@code today}: that of its {@link #currentSegment current segment}, or
     * {@code fallback} (the booking's own, i.e. last, room) when that segment has none. Not the booking's room: a move
     * planned for a later night changes that one at once and the guest's whereabouts only on the night.
     */
    public static String unitOn(List<BookingSegmentEntity> segments, String fallback, LocalDate today) {
        BookingSegmentEntity current = currentSegment(segments, today);
        return current != null && current.getRoomUnitId() != null ? current.getRoomUnitId() : fallback;
    }

    /**
     * The segment a stay is in on {@code today}: the one holding that night; before the first night
     * the first; from the departure day on, the last (the room an overstaying guest is in). Null
     * only when there are no segments.
     */
    public static BookingSegmentEntity currentSegment(List<BookingSegmentEntity> segments, LocalDate today) {
        List<BookingSegmentEntity> sorted = segments.stream().sorted(Comparator.comparing(BookingSegmentEntity::getCheckIn)).toList();
        if (sorted.isEmpty()) {
            return null;
        }
        return sorted.stream()
                .filter(s -> !today.isBefore(s.getCheckIn()) && today.isBefore(s.getCheckOut()))
                .findFirst()
                .orElse(today.isBefore(sorted.get(0).getCheckIn()) ? sorted.get(0) : sorted.get(sorted.size() - 1));
    }

    /** {@link #current()} narrowed to one room type and a {@code [checkIn, checkOut)} window. */
    public List<Overstay> overlapping(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return current().stream().filter(o -> o.roomId().equals(roomId) && o.overlaps(checkIn, checkOut)).toList();
    }
}
