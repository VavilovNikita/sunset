package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
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
 * The SERIALIZABLE-isolated write path for booking creation and room-unit assignment, split
 * into its own bean (rather than a private method on BookingService) so the @Transactional
 * proxy actually applies - Spring can't intercept a self-invoked call within the same instance.
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
        if (!isRangeAvailable(room.getId(), unitCount, checkIn, checkOut)) {
            throw new ConflictException("Selected dates are no longer available");
        }

        BookingEntity entity = new BookingEntity();
        entity.setRoomId(room.getId());
        entity.setGuestName(guestName);
        entity.setGuestEmail(guestEmail);
        entity.setGuestPhone(guestPhone);
        entity.setCheckIn(checkIn);
        entity.setCheckOut(checkOut);
        entity.setTotalPrice(computeTotalPrice(room, checkIn, checkOut));
        entity.setStatus(BookingStatus.NEW);
        return bookingRepository.saveAndFlush(entity);
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

        if (!unit.getRoomId().equals(booking.getRoomId())) {
            throw new BadRequestException("Room " + unit.getLabel() + " is a different room type than this booking");
        }
        if (!unit.isActive()) {
            throw new BadRequestException("Room " + unit.getLabel() + " is not active");
        }

        List<RoomUnitBlockEntity> blocks = roomUnitBlockRepository.findByRoomUnitIdAndFromDateLessThanAndToDateGreaterThanEqual(
                unit.getId(), booking.getCheckOut(), booking.getCheckIn());
        if (!blocks.isEmpty()) {
            RoomUnitBlockEntity block = blocks.get(0);
            throw new ConflictException("Room " + unit.getLabel() + " is blocked (" + block.getReason() + ") from " + block.getFromDate()
                    + " to " + block.getToDate());
        }

        List<BookingEntity> overlapping = bookingRepository.findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
                unit.getId(), BookingStatus.CANCELLED, booking.getCheckOut(), booking.getCheckIn(), booking.getId());
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Room " + unit.getLabel() + " is already booked for an overlapping stay");
        }

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
     * available(date) = activeUnitCount - distinct blocked units(date) - active bookings
     * covering date. A date is covered by a booking if checkIn &lt;= date &lt; checkOut (checkout
     * day is free). The range is available only if every night in [checkIn, checkOut) has at
     * least one unit left - this, plus the SERIALIZABLE isolation this method runs under, is
     * what stops two concurrent requests from both taking the last unit on the same date.
     */
    private boolean isRangeAvailable(String roomId, int unitCount, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);

        List<String> unitIds = roomUnitRepository.findByRoomIdAndIsActiveTrue(roomId).stream().map(RoomUnitEntity::getId).toList();
        List<RoomUnitBlockEntity> blocks = unitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        unitIds, checkOut.minusDays(1), checkIn);

        List<BookingEntity> overlapping = bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                roomId, BookingStatus.CANCELLED, checkOut, checkIn);

        for (LocalDate night : nights) {
            long blockedUnits = blocks.stream()
                    .filter(b -> !night.isBefore(b.getFromDate()) && !night.isAfter(b.getToDate()))
                    .map(RoomUnitBlockEntity::getRoomUnitId)
                    .distinct()
                    .count();
            long bookedUnits = overlapping.stream()
                    .filter(b -> !night.isBefore(b.getCheckIn()) && night.isBefore(b.getCheckOut()))
                    .count();
            int available = unitCount - (int) blockedUnits - (int) bookedUnits;
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
