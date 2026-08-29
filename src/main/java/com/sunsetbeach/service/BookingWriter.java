package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SERIALIZABLE-isolated write path for booking creation, room-unit assignment, and schedule
 * changes, split into its own bean (rather than a private method on BookingService) so the
 * @Transactional proxy actually applies - Spring can't intercept a self-invoked call within the
 * same instance.
 */
@Service
public class BookingWriter {

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final RatePlanRepository ratePlanRepository;

    public BookingWriter(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            RatePlanRepository ratePlanRepository) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insert(
            RoomEntity room, String guestName, String guestEmail, String guestPhone, LocalDate checkIn, LocalDate checkOut) {
        // Re-read the active unit count inside the SERIALIZABLE transaction rather than trusting
        // a pre-transaction value - this is the same count a concurrent RoomUnit activation/
        // deactivation would be changing, so it needs to participate in this transaction's
        // conflict detection like everything else isRangeAvailable reads.
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        BookingEntity entity = newBookingEntity(room, guestName, guestEmail, guestPhone, checkIn, checkOut, BookingSource.PUBLIC);
        return bookingRepository.saveAndFlush(entity);
    }

    /**
     * Staff-initiated counterpart of {@link #insert} for {@code POST /bookings/staff}: creates
     * the booking and, if {@code roomUnitId} is non-null, assigns the physical room in the same
     * SERIALIZABLE transaction. Unlike the public flow ({@code POST /bookings} followed by a
     * separate {@code PUT /bookings/{id}/room-unit}), there is no window between two writes
     * where the booking exists without the room that was asked for - either both succeed, or
     * the whole transaction rolls back and no booking is created at all.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insertStaff(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomUnitId) {
        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut, null)) {
            throw new ConflictException("Selected dates are no longer available");
        }
        BookingEntity entity = newBookingEntity(room, guestName, guestEmail, guestPhone, checkIn, checkOut, BookingSource.STAFF);

        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            // No booking id exists yet for this not-yet-persisted stay, so there is nothing of
            // its own to exclude from the overlap check (excludeBookingId=null).
            failIfUnassignable(checkUnitAssignable(unit, room.getId(), checkIn, checkOut, null));
            entity.setRoomUnitId(unit.getId());
        }
        return bookingRepository.saveAndFlush(entity);
    }

    private BookingEntity newBookingEntity(
            RoomEntity room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BookingSource source) {
        BookingEntity entity = new BookingEntity();
        entity.setRoomId(room.getId());
        entity.setGuestName(guestName);
        entity.setGuestEmail(guestEmail);
        entity.setGuestPhone(guestPhone);
        entity.setCheckIn(checkIn);
        entity.setCheckOut(checkOut);
        entity.setTotalPrice(computeTotalPrice(room, checkIn, checkOut));
        entity.setStatus(BookingStatus.NEW);
        entity.setSource(source);
        return entity;
    }

    /**
     * Assigns a physical room to a booking, guarding the same three things
     * {@code PUT /bookings/{id}/room-unit} promises: same room type, active, free for the whole
     * stay. Runs SERIALIZABLE, same as {@link #insert}, because two staff members can now race
     * for the same specific unit (not just the last unit of a type) - re-reads both rows fresh
     * so a concurrent conflicting assignment surfaces as a serialization failure rather than a
     * silent double-booking.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity assignRoomUnit(String bookingId, String roomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));

        failIfUnassignable(checkUnitAssignable(unit, booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut(), booking.getId()));

        booking.setRoomUnitId(unit.getId());
        return bookingRepository.saveAndFlush(booking);
    }

    /** No contention to protect against when clearing an assignment, so plain default isolation is enough. */
    @Transactional
    public BookingEntity unassignRoomUnit(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        booking.setRoomUnitId(null);
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Changes a booking's dates and/or physical room in one operation - the write path behind
     * {@code PATCH /bookings/{id}/schedule}. Re-reads everything fresh (unit count, blocks,
     * overlapping bookings) inside this SERIALIZABLE transaction, same as {@link #insert}/
     * {@link #assignRoomUnit}, and excludes this booking's own current reservation from every
     * conflict check - without that, extending a stay would spuriously conflict with the
     * booking's own prior dates, since its old row is still sitting in the table until this
     * save.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity updateSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        RoomEntity room = roomRepository.findById(booking.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, newCheckIn, newCheckOut, bookingId)) {
            throw new ConflictException("Selected dates are no longer available");
        }

        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            failIfUnassignable(checkUnitAssignable(unit, room.getId(), newCheckIn, newCheckOut, bookingId));
            booking.setRoomUnitId(unit.getId());
        } else {
            booking.setRoomUnitId(null);
        }

        booking.setCheckIn(newCheckIn);
        booking.setCheckOut(newCheckOut);
        booking.setTotalPrice(computeTotalPrice(room, newCheckIn, newCheckOut));
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Read-only, advisory preview for {@code POST /bookings/{id}/schedule/quote} - runs the
     * exact same checks as {@link #updateSchedule} (sharing {@link #checkUnitAssignable}/
     * {@link #isRangeAvailable} so the two cannot drift apart), but never writes and never
     * throws on an unavailable result: it reports {@code available=false} with a reason instead,
     * since "no" is a valid answer to a preview, not an error. Not SERIALIZABLE - this is
     * advisory only, the apply call is the sole source of truth and re-validates from scratch.
     */
    @Transactional(readOnly = true)
    public ScheduleQuote quoteSchedule(String bookingId, LocalDate newCheckIn, LocalDate newCheckOut, String roomUnitId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        RoomEntity room = roomRepository.findById(booking.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        BigDecimal totalPrice = computeTotalPrice(room, newCheckIn, newCheckOut);
        int nights = DateRangeUtil.getNights(newCheckIn, newCheckOut).size();

        int unitCount = (int) roomUnitRepository.countByRoomIdAndIsActiveTrue(room.getId());
        if (!isRangeAvailable(room.getId(), unitCount, newCheckIn, newCheckOut, bookingId)) {
            return new ScheduleQuote(totalPrice, nights, false, "Selected dates are no longer available");
        }

        if (roomUnitId != null) {
            RoomUnitEntity unit = roomUnitRepository.findById(roomUnitId).orElseThrow(() -> new NotFoundException("Room unit not found"));
            UnitConflict conflict = checkUnitAssignable(unit, room.getId(), newCheckIn, newCheckOut, bookingId);
            if (conflict != null) {
                return new ScheduleQuote(totalPrice, nights, false, conflict.message());
            }
        }

        return new ScheduleQuote(totalPrice, nights, true, null);
    }

    /** Result of {@link #quoteSchedule} - mirrors what {@link #updateSchedule} would do without writing. */
    public record ScheduleQuote(BigDecimal totalPrice, int nights, boolean available, String reason) {
    }

    private record UnitConflict(String message, boolean badRequest) {
    }

    private static void failIfUnassignable(UnitConflict conflict) {
        if (conflict == null) {
            return;
        }
        if (conflict.badRequest()) {
            throw new BadRequestException(conflict.message());
        }
        throw new ConflictException(conflict.message());
    }

    /**
     * The three checks {@code PUT /bookings/{id}/room-unit}, {@code PATCH /bookings/{id}/schedule}
     * and {@code POST /bookings/staff} all need before letting a physical room take a booking:
     * same room type, active, not blocked, not already booked by someone else for an overlapping
     * stay. {@code excludeBookingId} is the booking being (re)assigned so it never conflicts
     * with its own existing reservation - {@code null} only for a booking that doesn't exist yet
     * ({@link #insertStaff}), where there is nothing of its own to exclude.
     */
    private UnitConflict checkUnitAssignable(
            RoomUnitEntity unit, String bookingRoomId, LocalDate checkIn, LocalDate checkOut, String excludeBookingId) {
        if (!unit.getRoomId().equals(bookingRoomId)) {
            return new UnitConflict("Room " + unit.getLabel() + " is a different room type than this booking", true);
        }
        if (!unit.isActive()) {
            return new UnitConflict("Room " + unit.getLabel() + " is not active", true);
        }

        List<RoomUnitBlockEntity> blocks =
                roomUnitBlockRepository.findByRoomUnitIdAndFromDateLessThanAndToDateGreaterThanEqual(unit.getId(), checkOut, checkIn);
        if (!blocks.isEmpty()) {
            RoomUnitBlockEntity block = blocks.get(0);
            return new UnitConflict("Room " + unit.getLabel() + " is blocked (" + block.getReason() + ") from " + block.getFromDate()
                    + " to " + block.getToDate(), false);
        }

        List<BookingEntity> overlapping = excludeBookingId == null
                ? bookingRepository.findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn)
                : bookingRepository.findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
                        unit.getId(), BookingStatus.CANCELLED, checkOut, checkIn, excludeBookingId);
        if (!overlapping.isEmpty()) {
            return new UnitConflict("Room " + unit.getLabel() + " is already booked for an overlapping stay", false);
        }
        return null;
    }

    /**
     * available(date) = activeUnitCount - distinct blocked units(date) - active bookings
     * covering date. A date is covered by a booking if checkIn &lt;= date &lt; checkOut (checkout
     * day is free). The range is available only if every night in [checkIn, checkOut) has at
     * least one unit left - this, plus the SERIALIZABLE isolation this method runs under, is
     * what stops two concurrent requests from both taking the last unit on the same date.
     * {@code excludeBookingId}, when non-null, excludes that booking's own current reservation
     * from the overlap count - required for {@link #updateSchedule}/{@link #quoteSchedule} so a
     * booking extending/moving its own stay never conflicts with itself.
     */
    private boolean isRangeAvailable(String roomId, int unitCount, LocalDate checkIn, LocalDate checkOut, String excludeBookingId) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);

        List<String> unitIds = roomUnitRepository.findByRoomIdAndIsActiveTrue(roomId).stream().map(RoomUnitEntity::getId).toList();
        List<RoomUnitBlockEntity> blocks = unitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        unitIds, checkOut.minusDays(1), checkIn);

        List<BookingEntity> overlapping = excludeBookingId == null
                ? bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(roomId, BookingStatus.CANCELLED, checkOut, checkIn)
                : bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
                        roomId, BookingStatus.CANCELLED, checkOut, checkIn, excludeBookingId);

        for (LocalDate night : nights) {
            long blockedUnits = blocks.stream()
                    .filter(b -> !night.isBefore(b.getFromDate()) && !night.isAfter(b.getToDate()))
                    .map(RoomUnitBlockEntity::getRoomUnitId)
                    .distinct()
                    .count();
            long bookedUnits = overlapping.stream()
                    .filter(b -> !night.isBefore(b.getCheckIn()) && night.isBefore(b.getCheckOut()))
                    .count();
            int available = InventoryMath.availableCount(unitCount, (int) blockedUnits, (int) bookedUnits);
            if (available < 1) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal computeTotalPrice(RoomEntity room, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);

        Map<LocalDate, BigDecimal> overrides = new HashMap<>();
        for (RatePlanEntity plan : ratePlanRepository.findByRoomIdAndDateIn(room.getId(), nights)) {
            overrides.put(plan.getDate(), plan.getPrice());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate night : nights) {
            total = total.add(overrides.getOrDefault(night, room.getBasePrice()));
        }
        return total;
    }
}
