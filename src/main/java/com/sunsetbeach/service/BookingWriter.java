package com.sunsetbeach.service;

import com.sunsetbeach.entity.AvailabilityEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.AvailabilityRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RatePlanRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The SERIALIZABLE-isolated write path for booking creation, split into its own bean (rather
 * than a private method on BookingService) so the @Transactional proxy actually applies -
 * Spring can't intercept a self-invoked call within the same instance.
 */
@Service
public class BookingWriter {

    private final RoomRepository roomRepository;
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final RatePlanRepository ratePlanRepository;

    public BookingWriter(
            RoomRepository roomRepository,
            AvailabilityRepository availabilityRepository,
            BookingRepository bookingRepository,
            RatePlanRepository ratePlanRepository) {
        this.roomRepository = roomRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insert(
            RoomEntity room, String guestName, String guestEmail, String guestPhone, LocalDate checkIn, LocalDate checkOut) {
        // Re-read quantity inside the SERIALIZABLE transaction rather than trusting the
        // caller's (possibly pre-transaction) copy - this is the same value a concurrent
        // PATCH /rooms/{id} lowering quantity would be writing, so it needs to participate
        // in this transaction's conflict detection like everything else isRangeAvailable reads.
        int quantity = roomRepository.findById(room.getId()).map(RoomEntity::getQuantity).orElse(room.getQuantity());
        if (!isRangeAvailable(room.getId(), quantity, checkIn, checkOut)) {
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
     * available(date) = room.quantity - blockedCount(date) - active bookings covering date.
     * A date is covered by a booking if checkIn &lt;= date &lt; checkOut (checkout day is free).
     * The range is available only if every night in [checkIn, checkOut) has at least one unit
     * left - this, plus the SERIALIZABLE isolation this method runs under, is what stops two
     * concurrent requests from both taking the last unit on the same date.
     */
    private boolean isRangeAvailable(String roomId, int quantity, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = DateRangeUtil.getNights(checkIn, checkOut);

        Map<LocalDate, Integer> blockedByDate = new HashMap<>();
        for (AvailabilityEntity block : availabilityRepository.findByRoomIdAndDateBetween(roomId, checkIn, checkOut.minusDays(1))) {
            blockedByDate.put(block.getDate(), block.getBlockedCount());
        }

        List<BookingEntity> overlapping = bookingRepository.findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                roomId, BookingStatus.CANCELLED, checkOut, checkIn);

        for (LocalDate night : nights) {
            int blocked = blockedByDate.getOrDefault(night, 0);
            long booked = overlapping.stream()
                    .filter(b -> !night.isBefore(b.getCheckIn()) && night.isBefore(b.getCheckOut()))
                    .count();
            int available = quantity - blocked - (int) booked;
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
