package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.RoomStillHeldState;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    /** Which of two guests in one room is named: the longest overdue, then any stable order. */
    private static final Comparator<InHouseGuest> LONGEST_OVERDUE =
            Comparator.<InHouseGuest>comparingInt(InHouseGuest::overdueDays).thenComparing(g -> g.booking().getId());

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
     * within their stay (the departure day included). {@code awaitingRoom}: their move has taken effect with
     * no new room picked, so the room they are listed under is the one they have not left yet.
     * {@code inRoomSince}: the first night of the part of the stay that puts them in this room - of two guests in one,
     * the later date is the one who was moved in on top of the other.
     */
    public record InHouseGuest(BookingEntity booking, int overdueDays, boolean dueOutToday, boolean awaitingRoom, LocalDate inRoomSince) {

        public boolean overdue() {
            return overdueDays > 0;
        }

        /**
         * Why they are still there although the room should be free of them by today, in the order every sentence about
         * them uses; null for a guest simply staying.
         */
        public RoomStillHeldState holding() {
            return overdue() ? RoomStillHeldState.PAST_CHECK_OUT
                    : awaitingRoom ? RoomStillHeldState.AWAITING_ROOM
                    : dueOutToday ? RoomStillHeldState.DUE_OUT_TODAY
                    : null;
        }
    }

    /**
     * Who is physically in a room right now, keyed by room unit: every {@code CHECKED_IN},
     * not-cancelled booking under the unit {@link #unitOn} answers for today - an
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
            List<BookingSegmentEntity> segments = segmentsByBooking.getOrDefault(booking.getId(), List.of());
            String unitId = unitOn(booking, segments, today, clock);
            if (unitId != null) {
                byUnit.computeIfAbsent(unitId, k -> new ArrayList<>()).add(inHouseGuest(booking, segments, today));
            }
        }
        return byUnit;
    }

    private InHouseGuest inHouseGuest(BookingEntity booking, List<BookingSegmentEntity> segments, LocalDate today) {
        BookingSegmentEntity holder = roomSegmentOn(booking, segments, today, clock);
        return new InHouseGuest(booking, overdueDays(booking, today), booking.getCheckOut().equals(today), awaitsRoom(booking, segments, today),
                holder == null ? booking.getCheckIn() : holder.getCheckIn());
    }

    /**
     * The checked-in guest who sleeps in {@code roomUnitId} tonight although no part of their stay says so - their move
     * took effect with no new room picked - leaving out {@code except}. Not one due out today: that night is free to
     * sell, as for anybody's departure day. The one case an assignment's overlap query cannot see; asked narrowly (only
     * stays whose part in this room has ended), since it runs inside the SERIALIZABLE availability writes.
     */
    public Optional<InHouseGuest> stillLivingIn(String roomUnitId, Set<String> except) {
        LocalDate today = today();
        List<String> candidates = segmentRepository
                .findByRoomUnitIdAndBooking_OccupancyStatusAndCheckOutLessThanEqual(roomUnitId, OccupancyStatus.CHECKED_IN, today).stream()
                .map(BookingSegmentEntity::getBookingId)
                .distinct()
                .filter(id -> !except.contains(id))
                .toList();
        for (BookingEntity booking : bookingRepository.findAllById(candidates)) {
            List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(booking.getId());
            if (booking.getStatus() != BookingStatus.CANCELLED
                    && !booking.getCheckOut().equals(today)
                    && awaitsRoom(booking, segments, today)
                    && roomUnitId.equals(unitOn(booking, segments, today, clock))) {
                return Optional.of(inHouseGuest(booking, segments, today));
            }
        }
        return Optional.empty();
    }

    /**
     * A checked-in guest who sleeps on {@code night} in the room of an earlier part of their stay, their new room not
     * picked: {@code heldIn} is that part. The calendar and the per-room availability read this, so they show the room
     * taken as the map and Today do - the room type's count already has the night under the new part.
     */
    public record WaitingGuest(BookingEntity booking, BookingSegmentEntity heldIn, LocalDate night) {

        public String roomUnitId() {
            return heldIn.getRoomUnitId();
        }
    }

    /**
     * Every {@link WaitingGuest} tonight. Not one due out today: that night is free to sell, as on anybody's departure
     * day. Nobody to report for a guest with no earlier room to be in.
     */
    public List<WaitingGuest> waitingTonight() {
        LocalDate today = today();
        List<BookingEntity> inHouse =
                bookingRepository.findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED);
        if (inHouse.isEmpty()) {
            return List.of();
        }
        Map<String, List<BookingSegmentEntity>> segmentsByBooking = segmentRepository
                .findByBookingIdIn(inHouse.stream().map(BookingEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));
        List<WaitingGuest> waiting = new ArrayList<>();
        for (BookingEntity booking : inHouse) {
            List<BookingSegmentEntity> segments = segmentsByBooking.getOrDefault(booking.getId(), List.of());
            if (booking.getCheckOut().equals(today) || !awaitsRoom(booking, segments, today)) {
                continue;
            }
            BookingSegmentEntity heldIn = roomSegmentOn(booking, segments, today, clock);
            if (heldIn.getRoomUnitId() != null) {
                waiting.add(new WaitingGuest(booking, heldIn, today));
            }
        }
        return waiting;
    }

    /** A room somebody was moved into while {@code holder}, who should have left it by now, is still there. */
    public record DoubledRoom(String roomUnitId, InHouseGuest holder) {}

    /**
     * Whether {@code bookingId} is the later of two guests in a room: moved into it ({@link InHouseGuest#inRoomSince})
     * while one who came before is still there - past check-out, due out today, or waiting for a new room ({@link
     * InHouseGuest#holding}). A planned move takes effect by date, so nothing refuses it - this is what the screens warn
     * with. Gone once the earlier guest is checked out, or either one's room is changed.
     */
    public static Optional<DoubledRoom> roomStillHeld(Map<String, List<InHouseGuest>> byUnit, String bookingId) {
        for (Map.Entry<String, List<InHouseGuest>> room : byUnit.entrySet()) {
            for (InHouseGuest guest : room.getValue()) {
                if (guest.booking().getId().equals(bookingId)) {
                    return room.getValue().stream()
                            .filter(other -> other.holding() != null && other.inRoomSince().isBefore(guest.inRoomSince()))
                            .max(LONGEST_OVERDUE)
                            .map(holder -> new DoubledRoom(room.getKey(), holder));
                }
            }
        }
        return Optional.empty();
    }

    public static String stillHeldMessage(String unitLabel, InHouseGuest holder) {
        String why = switch (holder.holding()) {
            case PAST_CHECK_OUT -> "past check-out";
            case AWAITING_ROOM -> "waiting for a new room";
            case DUE_OUT_TODAY -> "due out today";
        };
        return "Room " + unitLabel + " is still held by " + holder.booking().getGuestName() + " (" + why + ")";
    }

    /**
     * Whether somebody other than {@code bookingId} who should have left {@code roomUnitId} by now ({@link
     * InHouseGuest#holding}) is still in it: another guest's move out of it does not make it a room to clean. Their own
     * check-out, or picking their new room, does.
     */
    public static boolean stillHeldByAnother(Map<String, List<InHouseGuest>> byUnit, String roomUnitId, String bookingId) {
        return byUnit.getOrDefault(roomUnitId, List.of()).stream()
                .anyMatch(g -> g.holding() != null && !g.booking().getId().equals(bookingId));
    }

    /**
     * The guest a room's tile stands for, where only one can: of several, the one who came last (who carries the {@link
     * #roomStillHeld} warning about the other), then the longest overdue.
     */
    public static Optional<InHouseGuest> shownIn(Map<String, List<InHouseGuest>> byUnit, String roomUnitId) {
        return byUnit.getOrDefault(roomUnitId, List.of()).stream()
                .max(Comparator.comparing(InHouseGuest::inRoomSince).thenComparing(LONGEST_OVERDUE));
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
                .max(LONGEST_OVERDUE);
    }

    /**
     * The one sentence for a room another guest is still checked into; {@code whatToDo} is the caller's way out - except
     * for a guest whose new room is not picked yet, where the way out is to pick it.
     */
    public static String occupiedMessage(String unitLabel, InHouseGuest other, String whatToDo) {
        String state = other.overdue() ? "past check-out" : other.awaitingRoom() ? "new room not chosen yet" : other.dueOutToday() ? "due out today" : null;
        return "Room " + unitLabel + " is still occupied by " + other.booking().getGuestName() + " (checked in"
                + (state == null ? "" : ", " + state) + "). "
                + (other.awaitingRoom() && !other.overdue() ? "Choose their new room first, or use another room." : whatToDo);
    }

    /**
     * The physical room a stay is in on {@code date}: that of {@link #roomSegmentOn}, null when there is none. Not the
     * booking's own room: that is the last segment's, which a move planned for a later night changes at once and the
     * guest's whereabouts only on the night - so it never stands in for a night without a room (it used to, and an
     * arrival with no room tonight was checked into the later one). {@code booking.roomUnitId} answers only for a
     * booking with no segments at all. {@code clock} says which part of the stay has begun.
     */
    public static String unitOn(BookingEntity booking, List<BookingSegmentEntity> segments, LocalDate date, Clock clock) {
        BookingSegmentEntity holder = roomSegmentOn(booking, segments, date, clock);
        return holder != null ? holder.getRoomUnitId() : booking.getRoomUnitId();
    }

    /**
     * The segment that says which room a stay is in on {@code date}: its {@link #currentSegment current segment} - but
     * a guest who is checked in stays where they are until a new room is picked. When the part holding {@code date} has
     * begun and has no room, it is the latest earlier part with a room that they slept in; with none, the part itself
     * (no room). A part that has not begun yet is never filled in: where they will be then is still open. Null only when
     * there are no segments.
     */
    public static BookingSegmentEntity roomSegmentOn(BookingEntity booking, List<BookingSegmentEntity> segments, LocalDate date, Clock clock) {
        BookingSegmentEntity holder = currentSegment(segments, date);
        if (holder == null
                || holder.getRoomUnitId() != null
                || booking.getOccupancyStatus() != OccupancyStatus.CHECKED_IN
                || holder.getCheckIn().isAfter(LocalDate.now(clock))) {
            return holder;
        }
        // The hotel day they came: a part that had ended by then was never slept in (a late arrival).
        LocalDate arrived = booking.getCheckedInAt() == null
                ? null
                : booking.getCheckedInAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate();
        return segments.stream()
                .filter(s -> s.getCheckIn().isBefore(holder.getCheckIn()) && s.getRoomUnitId() != null)
                .filter(s -> arrived == null || arrived.isBefore(s.getCheckOut()))
                .max(Comparator.comparing(BookingSegmentEntity::getCheckIn))
                .orElse(holder);
    }

    /** A guest in the house whose part of the stay holding {@code today} has no room: someone has to pick it. */
    public static boolean awaitsRoom(BookingEntity booking, List<BookingSegmentEntity> segments, LocalDate today) {
        BookingSegmentEntity current = currentSegment(segments, today);
        return booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN
                && booking.getStatus() != BookingStatus.CANCELLED
                && current != null
                && current.getRoomUnitId() == null;
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
