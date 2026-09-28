package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GuestEmailLogEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.LifecycleEmailType;
import com.sunsetbeach.model.OccupancyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The log itself plus the sweep's candidate queries - see {@code LifecycleEmailService}. Every
 * candidate query carries the same audience rule: the booking's {@code Guest} card is linked to a
 * {@code GuestAccount} with a verified email that hasn't opted out. A card with no account never
 * matches, because the join to {@code GuestAccountEntity} is an inner one.
 */
public interface GuestEmailLogRepository extends JpaRepository<GuestEmailLogEntity, String> {

    List<GuestEmailLogEntity> findByGuestIdOrderBySentAtDesc(String guestId);

    /**
     * Bookings checking in on {@code checkIn}, not cancelled, with no {@code type} email logged for
     * them yet. {@code excludedOccupancy} lets post-stay leave out a no-show; pass a value that can't
     * apply (pre-arrival passes {@code NO_SHOW} too - nobody is a no-show before arriving) rather
     * than a second query.
     */
    @Query("""
        SELECT new com.sunsetbeach.repository.StayCandidate(
            b.guestId, b.id, a.email, a.unsubscribeToken, COALESCE(a.name, g.name), r.name, b.checkIn, b.checkOut)
        FROM BookingEntity b
        JOIN b.room r
        JOIN GuestEntity g ON g.id = b.guestId
        JOIN GuestAccountEntity a ON a.guestId = b.guestId
        WHERE b.checkIn = :checkIn
          AND b.status <> :cancelled
          AND b.occupancyStatus <> :excludedOccupancy
          AND a.emailVerifiedAt IS NOT NULL
          AND a.marketingEmailsOptOut = false
          AND NOT EXISTS (
              SELECT 1 FROM GuestEmailLogEntity l
              WHERE l.guestId = b.guestId AND l.bookingId = b.id AND l.type = :type)
        """)
    List<StayCandidate> findStayCandidatesByCheckIn(
            @Param("checkIn") LocalDate checkIn,
            @Param("cancelled") BookingStatus cancelled,
            @Param("excludedOccupancy") OccupancyStatus excludedOccupancy,
            @Param("type") LifecycleEmailType type);

    /** Same as {@link #findStayCandidatesByCheckIn}, keyed on {@code checkOut}. */
    @Query("""
        SELECT new com.sunsetbeach.repository.StayCandidate(
            b.guestId, b.id, a.email, a.unsubscribeToken, COALESCE(a.name, g.name), r.name, b.checkIn, b.checkOut)
        FROM BookingEntity b
        JOIN b.room r
        JOIN GuestEntity g ON g.id = b.guestId
        JOIN GuestAccountEntity a ON a.guestId = b.guestId
        WHERE b.checkOut = :checkOut
          AND b.status <> :cancelled
          AND b.occupancyStatus <> :excludedOccupancy
          AND a.emailVerifiedAt IS NOT NULL
          AND a.marketingEmailsOptOut = false
          AND NOT EXISTS (
              SELECT 1 FROM GuestEmailLogEntity l
              WHERE l.guestId = b.guestId AND l.bookingId = b.id AND l.type = :type)
        """)
    List<StayCandidate> findStayCandidatesByCheckOut(
            @Param("checkOut") LocalDate checkOut,
            @Param("cancelled") BookingStatus cancelled,
            @Param("excludedOccupancy") OccupancyStatus excludedOccupancy,
            @Param("type") LifecycleEmailType type);

    /**
     * Guests whose most recent non-cancelled booking checked out before {@code lastCheckOutBefore}
     * (so a guest with any upcoming or recent stay never matches), with no {@code winBack} email
     * logged at or after {@code noWinBackSince}.
     */
    @Query("""
        SELECT new com.sunsetbeach.repository.WinBackCandidate(
            a.guestId, a.email, a.unsubscribeToken, COALESCE(a.name, g.name), MAX(b.checkOut))
        FROM GuestAccountEntity a
        JOIN GuestEntity g ON g.id = a.guestId
        JOIN BookingEntity b ON b.guestId = a.guestId
        WHERE a.emailVerifiedAt IS NOT NULL
          AND a.marketingEmailsOptOut = false
          AND b.status <> :cancelled
          AND NOT EXISTS (
              SELECT 1 FROM GuestEmailLogEntity l
              WHERE l.guestId = a.guestId AND l.type = :winBack AND l.sentAt >= :noWinBackSince)
        GROUP BY a.guestId, a.email, a.unsubscribeToken, a.name, g.name
        HAVING MAX(b.checkOut) < :lastCheckOutBefore
        """)
    List<WinBackCandidate> findWinBackCandidates(
            @Param("lastCheckOutBefore") LocalDate lastCheckOutBefore,
            @Param("noWinBackSince") LocalDateTime noWinBackSince,
            @Param("cancelled") BookingStatus cancelled,
            @Param("winBack") LifecycleEmailType winBack);
}
