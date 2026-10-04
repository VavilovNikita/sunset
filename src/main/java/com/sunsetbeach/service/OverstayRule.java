package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        /**
         * Overstay nights inside {@code [rangeFrom, rangeToExclusive)}, {@code 0} if none. The one
         * count of them: the room reports' zero-revenue slice and the night audit's unpaid-nights
         * warning both read it, so they can't disagree about how many nights a guest overstayed.
         */
        public long nightsIn(LocalDate rangeFrom, LocalDate rangeToExclusive) {
            LocalDate start = from.isAfter(rangeFrom) ? from : rangeFrom;
            LocalDate end = toExclusive.isBefore(rangeToExclusive) ? toExclusive : rangeToExclusive;
            return start.isBefore(end) ? ChronoUnit.DAYS.between(start, end) : 0;
        }
    }

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final Clock clock;

    public OverstayRule(BookingRepository bookingRepository, BookingSegmentRepository segmentRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.clock = clock;
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

    /** {@link #current()} narrowed to one room type and a {@code [checkIn, checkOut)} window. */
    public List<Overstay> overlapping(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return current().stream().filter(o -> o.roomId().equals(roomId) && o.overlaps(checkIn, checkOut)).toList();
    }
}
