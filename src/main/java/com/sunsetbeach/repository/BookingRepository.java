package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.model.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<BookingEntity, String>, JpaSpecificationExecutor<BookingEntity> {

    List<BookingEntity> findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    List<BookingEntity> findByRoomIdAndStatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate monthEnd, LocalDate monthStart);

    List<BookingEntity> findByRoomId(String roomId);

    /**
     * Bookings assigned to a specific unit that overlap its half-open stay window, excluding
     * this booking itself - the race check {@link com.sunsetbeach.service.BookingWriter#assignRoomUnit}
     * runs before assigning a unit.
     */
    List<BookingEntity> findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
            String roomUnitId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn, String excludedId);

    /**
     * Used by {@link com.sunsetbeach.service.RoomUnitService} to reject deleting/deactivating a
     * unit that's still promised to a future guest.
     */
    boolean existsByRoomUnitIdAndStatusNotAndCheckOutGreaterThan(String roomUnitId, BookingStatus excludedStatus, LocalDate from);
}
