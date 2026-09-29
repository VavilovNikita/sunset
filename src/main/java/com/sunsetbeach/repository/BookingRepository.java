package com.sunsetbeach.repository;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, String>, JpaSpecificationExecutor<BookingEntity> {

    List<BookingEntity> findByRoomId(String roomId);

    /** GET /reports/revenue-export's informational "Paid bookings" sheet - {@code checkOut} inclusive on both ends. */
    List<BookingEntity> findByStatusAndCheckOutBetween(BookingStatus status, LocalDate from, LocalDate to);

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

    /**
     * {@code GET /night-audit}'s missed arrivals - on or before the reviewed date, not only on it,
     * so an unresolved arrival from an earlier day stays listed (see {@code NightAuditService}).
     */
    List<BookingEntity> findByOccupancyStatusAndStatusNotAndCheckInLessThanEqualOrderByCheckInAsc(
            OccupancyStatus occupancyStatus, BookingStatus excludedStatus, LocalDate checkIn);

    /** {@code GET /night-audit}'s missed departures - same on-or-before rule as the arrivals query above. */
    List<BookingEntity> findByOccupancyStatusAndCheckOutLessThanEqualOrderByCheckOutAsc(OccupancyStatus occupancyStatus, LocalDate checkOut);

    /** {@code GET /reports/manager}'s arrivals - every occupancy status. {@code Is} for the same derivation reason as above. */
    List<BookingEntity> findByStatusNotAndCheckInIs(BookingStatus excludedStatus, LocalDate checkIn);

    /** {@code GET /reports/manager}'s departures - every occupancy status. */
    List<BookingEntity> findByStatusNotAndCheckOut(BookingStatus excludedStatus, LocalDate checkOut);

    /**
     * {@code GET /reports/manager}'s cancellations - {@code updatedAt} is the only timestamp a
     * status change leaves, so this is "cancelled now, last touched in the window."
     */
    long countByStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(BookingStatus status, LocalDateTime from, LocalDateTime toExclusive);

    /** {@code GET /reports/manager}'s no-shows - same {@code updatedAt} approximation as the cancellations count above. */
    long countByOccupancyStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            OccupancyStatus occupancyStatus, LocalDateTime from, LocalDateTime toExclusive);

    /** A guest's stay history for {@code GET /guests/{id}}, newest first - every status, cancelled included, see {@code GuestDetail}. */
    List<BookingEntity> findByGuestIdOrderByCreatedAtDesc(String guestId);

    /**
     * Fallback for {@code GET /guest/bookings} only, for an account not linked to a Guest card -
     * the primary path is {@link #findByGuestIdOrderByCreatedAtDesc}, see
     * {@code GuestAccountService#listBookings}. Case-insensitive since {@code guestEmail} is
     * whatever a guest typed at booking time, not normalized to any particular case.
     */
    List<BookingEntity> findByGuestEmailIgnoreCaseOrderByCreatedAtDesc(String guestEmail);

    /**
     * {@code GET /reports/guest-ltv}'s ranking - one row per guest card with at least one booking
     * whose status isn't {@code excludedStatus}, highest summed {@code totalPrice} first. The
     * {@code guestId} tie-break only keeps the order stable between calls; the page size is the
     * report's {@code limit}.
     */
    @Query("""
        SELECT b.guestId AS guestId, COUNT(b) AS bookingCount, SUM(b.totalPrice) AS roomRevenue,
               MIN(b.checkIn) AS firstCheckIn, MAX(b.checkIn) AS lastCheckIn
        FROM BookingEntity b
        WHERE b.guestId IS NOT NULL AND b.status <> :excludedStatus
        GROUP BY b.guestId
        ORDER BY SUM(b.totalPrice) DESC, b.guestId ASC
        """)
    List<GuestBookingTotals> sumByGuest(@Param("excludedStatus") BookingStatus excludedStatus, Pageable page);

    List<BookingEntity> findByGuestIdInAndStatusNot(Collection<String> guestIds, BookingStatus excludedStatus);

    interface GuestBookingTotals {
        String getGuestId();

        long getBookingCount();

        BigDecimal getRoomRevenue();

        LocalDate getFirstCheckIn();

        LocalDate getLastCheckIn();
    }

    /** {@code SELECT ... FOR UPDATE} - see {@code BookingService#updateStatus}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.id = :id")
    Optional<BookingEntity> findByIdForUpdate(@Param("id") String id);
}
