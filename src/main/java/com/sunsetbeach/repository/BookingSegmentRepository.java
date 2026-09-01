package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.model.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSegmentRepository extends JpaRepository<BookingSegmentEntity, String> {

    List<BookingSegmentEntity> findByBookingIdOrderByCheckInAsc(String bookingId);

    /** Bulk counterpart of {@link #findByBookingIdOrderByCheckInAsc} for {@code GET /bookings} - one query for a whole listed page, not one per row. */
    List<BookingSegmentEntity> findByBookingIdIn(List<String> bookingIds);

    /** Type-level overlap test, replacing the old Booking-based query of the same name/shape. */
    List<BookingSegmentEntity> findByRoomIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    /** Same, excluding one booking's own segments - a booking's own reservation never conflicts with itself. */
    List<BookingSegmentEntity> findByRoomIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
            String roomId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn, String excludedBookingId);

    /**
     * Same overlap shape as {@link #findByRoomIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan}
     * but with an inclusive checkIn bound - {@link com.sunsetbeach.service.AvailabilityService}'s
     * month-scoped query, kept exactly as the pre-segments version was (a month boundary query,
     * not a stay-overlap query, so it uses <= on purpose).
     */
    List<BookingSegmentEntity> findByRoomIdAndBooking_StatusNotAndCheckInLessThanEqualAndCheckOutGreaterThan(
            String roomId, BookingStatus excludedStatus, LocalDate monthEnd, LocalDate monthStart);

    /** Global (every room type at once) overlap query for {@code GET /bookings/calendar}. */
    List<BookingSegmentEntity> findByBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    /** Unit-level overlap, excluding one booking's own segments - the race check before assigning/relocating a unit. */
    List<BookingSegmentEntity> findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThanAndBookingIdNot(
            String roomUnitId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn, String excludedBookingId);

    /** Same, without excluding a booking - used when the booking being written doesn't exist yet (fresh insert). */
    List<BookingSegmentEntity> findByRoomUnitIdAndBooking_StatusNotAndCheckInLessThanAndCheckOutGreaterThan(
            String roomUnitId, BookingStatus excludedStatus, LocalDate checkOut, LocalDate checkIn);

    /** Used by {@link com.sunsetbeach.service.RoomUnitService} to reject deleting/deactivating a unit still promised to a future guest. */
    boolean existsByRoomUnitIdAndBooking_StatusNotAndCheckOutGreaterThan(String roomUnitId, BookingStatus excludedStatus, LocalDate from);
}
