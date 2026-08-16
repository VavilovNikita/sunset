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

    /** Used by {@link com.sunsetbeach.service.RoomService} to find the peak future commitment before shrinking `quantity`. */
    List<BookingEntity> findByRoomIdAndStatusNotAndCheckOutGreaterThan(String roomId, BookingStatus excludedStatus, LocalDate from);

    List<BookingEntity> findByRoomId(String roomId);
}
