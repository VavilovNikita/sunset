package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RatePlanEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.AvailabilityRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RatePlanRepository;
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

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final RatePlanRepository ratePlanRepository;

    public BookingWriter(
            AvailabilityRepository availabilityRepository, BookingRepository bookingRepository, RatePlanRepository ratePlanRepository) {
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingEntity insert(
            RoomEntity room, String guestName, String guestEmail, String guestPhone, LocalDate checkIn, LocalDate checkOut) {
        if (!isRangeAvailable(room.getId(), checkIn, checkOut)) {
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

    private boolean isRangeAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        long blockedCount = availabilityRepository.countByRoomIdAndIsBlockedTrueAndDateGreaterThanEqualAndDateLessThan(
                roomId, checkIn, checkOut);
        long overlappingCount = bookingRepository.countByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
                roomId, BookingStatus.CANCELLED, checkOut, checkIn);
        return blockedCount == 0 && overlappingCount == 0;
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
