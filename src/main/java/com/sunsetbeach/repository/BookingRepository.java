package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<BookingEntity, String>, JpaSpecificationExecutor<BookingEntity> {

    List<BookingEntity> findByRoomId(String roomId);

    /**
     * Every unconfirmed public booking, regardless of age - {@link com.sunsetbeach.service.BookingExpiryService}
     * needs to run its own business-day-aware date math per row (skipping weekends), which isn't
     * expressible as a single SQL cutoff predicate. This table is small for a hotel of this size,
     * so fetching the whole (normally tiny or empty) NEW/PUBLIC set every sweep is cheap.
     */
    List<BookingEntity> findByStatusAndSource(BookingStatus status, BookingSource source);

    /**
     * Arriving-today list for {@code GET /bookings/today} - see {@code BookingOccupancyService}.
     * The trailing {@code Is} is required, not decorative: Spring Data's query-derivation parser
     * always reads a property ending in "In" as the start of the {@code IN(...)} keyword unless
     * an explicit operator keyword is attached - plain {@code ...AndCheckIn(LocalDate)} fails to
     * derive at all ("No property 'check' found... Did you mean 'checkIn'"), because it tries to
     * parse "Check" as a property name expecting a collection-typed argument next.
     */
    List<BookingEntity> findByOccupancyStatusAndStatusNotAndCheckInIs(
            OccupancyStatus occupancyStatus, BookingStatus excludedStatus, LocalDate checkIn);

    /** Departing-today list for {@code GET /bookings/today} - {@code checkOut} has no such conflict, but kept explicit for symmetry with {@link #findByOccupancyStatusAndStatusNotAndCheckInIs}. */
    List<BookingEntity> findByOccupancyStatusAndStatusNotAndCheckOut(
            OccupancyStatus occupancyStatus, BookingStatus excludedStatus, LocalDate checkOut);

    /** In-house list for {@code GET /bookings/today} - every currently checked-in guest, regardless of checkOut date. */
    List<BookingEntity> findByOccupancyStatusAndStatusNot(OccupancyStatus occupancyStatus, BookingStatus excludedStatus);

    /** A guest's stay history for {@code GET /guests/{id}}, newest first - every status, cancelled included, see {@code GuestDetail}. */
    List<BookingEntity> findByGuestIdOrderByCreatedAtDesc(String guestId);
}
