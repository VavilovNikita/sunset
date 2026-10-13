package com.sunsetbeach.repository;

import com.sunsetbeach.entity.GymReservationEntity;
import com.sunsetbeach.model.GymReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GymReservationRepository extends JpaRepository<GymReservationEntity, String> {

    List<GymReservationEntity> findByBookingId(String bookingId);

    /**
     * The booking of a reservation without loading the reservation: callers lock the booking row
     * first and read the reservation afterwards, so the copy they work on is read after the lock.
     */
    @Query("select r.bookingId from GymReservationEntity r where r.id = :id")
    Optional<String> findBookingIdById(@Param("id") String id);

    List<GymReservationEntity> findByBookingIdIn(Collection<String> bookingIds);

    /** The schedule's own query: every reservation in a date range, ordered the way the grid reads. */
    List<GymReservationEntity> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    List<GymReservationEntity> findByDateBetweenAndStatusInOrderByDateAscStartTimeAsc(
            LocalDate from, LocalDate to, Collection<GymReservationStatus> statuses);

    /** Reservations of one booking on one date that hold the slot - the daily allowance's input. */
    List<GymReservationEntity> findByBookingIdAndDateAndStatusIn(String bookingId, LocalDate date, Collection<GymReservationStatus> statuses);

    /** What the automation still has to look at: unfinished reservations up to and including {@code date}. */
    List<GymReservationEntity> findByAutomationDoneFalseAndDateLessThanEqualOrderByDateAscStartTimeAsc(LocalDate date);

    /**
     * Status changes made by the automation go through conditional updates, never a full-entity
     * save: a staff member cancelling at the same instant must not be overwritten by a stale copy.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GymReservationEntity r set r.status = :to where r.id = :id and r.status = :from")
    int changeStatus(@Param("id") String id, @Param("from") GymReservationStatus from, @Param("to") GymReservationStatus to);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GymReservationEntity r set r.automationDone = true where r.id = :id")
    int markAutomationDone(@Param("id") String id);
}
