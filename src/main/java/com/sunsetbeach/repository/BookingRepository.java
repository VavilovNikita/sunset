package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.model.BookingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<BookingEntity, String>, JpaSpecificationExecutor<BookingEntity> {

    List<BookingEntity> findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    /**
     * Same overlap test as {@link #findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan},
     * excluding one booking - used by {@link com.sunsetbeach.service.BookingWriter#updateSchedule}
     * so a booking's own current reservation never counts against itself when extending/moving it.
     */
    List<BookingEntity> findByRoomIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
            String roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn, String excludedId);

    List<BookingEntity> findByRoomIdAndStatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate monthEnd, LocalDate monthStart);

    /** Global (every room type at once) overlap query for {@code GET /bookings/calendar}. */
    List<BookingEntity> findByStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    List<BookingEntity> findByRoomId(String roomId);

    /**
     * Bookings assigned to a specific unit that overlap its half-open stay window, excluding
     * this booking itself - the race check {@link com.sunsetbeach.service.BookingWriter#assignRoomUnit}
     * runs before assigning a unit.
     */
    List<BookingEntity> findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot(
            String roomUnitId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn, String excludedId);

    /**
     * Same overlap test as {@link #findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThanAndIdNot},
     * without excluding a booking - used by {@link com.sunsetbeach.service.BookingWriter#insertStaff}
     * to check a candidate unit before the new booking itself has an id to exclude.
     */
    List<BookingEntity> findByRoomUnitIdAndStatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            String roomUnitId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    /**
     * Used by {@link com.sunsetbeach.service.RoomUnitService} to reject deleting/deactivating a
     * unit that's still promised to a future guest.
     */
    boolean existsByRoomUnitIdAndStatusNotAndCheckOutGreaterThan(String roomUnitId, BookingStatus excludedStatus, LocalDate from);

    /** Unconfirmed public bookings past their hold window - see {@link com.sunsetbeach.service.BookingExpiryService}. */
    List<BookingEntity> findByStatusAndSourceAndCreatedAtBefore(BookingStatus status, BookingSource source, LocalDateTime cutoff);
}
