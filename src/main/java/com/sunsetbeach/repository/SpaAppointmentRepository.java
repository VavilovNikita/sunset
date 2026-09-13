package com.sunsetbeach.repository;

import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.model.SpaAppointmentStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SpaAppointmentRepository extends JpaRepository<SpaAppointmentEntity, String> {

    /** The grid's own query - everything on one date, regardless of status. */
    List<SpaAppointmentEntity> findByDate(LocalDate date);

    /**
     * Future, still-BOOKED appointments for one therapist - used to warn (not block) when they
     * are deactivated or lose the THERAPIST function. See {@link com.sunsetbeach.service.UserService}.
     */
    List<SpaAppointmentEntity> findByTherapistUserIdAndStatusAndDateGreaterThanEqual(
            String therapistUserId, SpaAppointmentStatus status, LocalDate date);

    /**
     * Table-axis candidates for auto-linking a newly opened POS order to a treatment - see
     * {@link com.sunsetbeach.service.OrderService#linkSpaAppointment}. {@code BOOKED}/
     * {@code COMPLETED} both count (the order can legitimately be opened before or after the
     * therapist marks the treatment done); {@code orderId IS NULL} so an already-billed
     * appointment is never a candidate for a second order. Resolved by time (which of today's
     * candidates the order-open moment actually falls in/near), not by count - see that method's
     * own javadoc for why a busy table needs this, not "exactly one for the day".
     */
    List<SpaAppointmentEntity> findByTableIdAndDateAndOrderIdIsNullAndStatusIn(
            String tableId, LocalDate date, List<SpaAppointmentStatus> statuses);

    /**
     * Booking-axis candidates for the same auto-link, tried first - see
     * {@link com.sunsetbeach.service.OrderService#linkSpaAppointment}. A room-charge order names
     * a booking but often no table at all, so the table-axis query above never fires for it. No
     * time signal is available here (the order isn't tied to a specific slot), so this stays
     * count-based: exactly one unlinked candidate for the day is unambiguous, two or more decline.
     */
    List<SpaAppointmentEntity> findByBookingIdAndDateAndOrderIdIsNullAndStatusIn(
            String bookingId, LocalDate date, List<SpaAppointmentStatus> statuses);

    /**
     * Guards {@link com.sunsetbeach.service.OrderService#autoLinkSpaAppointment} against
     * re-resolving an order that's already linked - auto-resolution runs on every
     * {@code addItems} call (items can arrive in more than one batch), and without this check a
     * second call would look for a *different* still-unlinked candidate near the same
     * table/booking (the one already linked no longer qualifies, having a non-null orderId) and
     * could wrongly attach this same order to a second appointment.
     */
    boolean existsByOrderId(String orderId);

    /**
     * Pushes both {@code SpaAppointment} exclusion constraints' checks to the end of the current
     * transaction instead of the end of the current statement - the primitive
     * {@link com.sunsetbeach.service.SpaAppointmentService#swapTables} depends on. A genuine swap
     * (two appointments trading tables) can only be expressed as two ordinary {@code UPDATE}s,
     * and under the default {@code NOT DEFERRABLE} timing each one is checked as it runs - the
     * first would find the second appointment still sitting on the table it's moving into and
     * fail, regardless of which one goes first, since neither table is free until both rows have
     * moved. A single statement moving both rows was tried and still fails the same way (see
     * {@code SpaAppointmentTableSwapConstraintTimingTests}) - {@code NOT DEFERRABLE} constraints
     * are enforced synchronously per row as each row's index entry is written, not once at the
     * end of the statement. Requires V56's migration ({@code DEFERRABLE INITIALLY IMMEDIATE}) -
     * {@code SET CONSTRAINTS} on a name that isn't actually deferrable is itself an error.
     *
     * <p>Only in effect for the transaction that calls it - see {@link #restoreImmediateOverlapConstraints()},
     * which every caller of this method must call before returning, so a genuine conflict still
     * surfaces as a catchable exception at a point this code controls, not as an opaque failure
     * at the outer transaction's own commit after the calling method has already returned.
     */
    @Modifying
    @Query(value = "SET CONSTRAINTS spa_appointment_no_table_overlap, spa_appointment_no_therapist_overlap DEFERRED", nativeQuery = true)
    void deferOverlapConstraints();

    /**
     * Forces the deferred checks queued by {@link #deferOverlapConstraints()} to run right now,
     * inside this same transaction, instead of waiting for commit. If the two appointments' final
     * table/time/therapist state is genuinely conflict-free (with each other, and with every
     * other row), this returns normally. If not - a real race, or a bug upstream that let this
     * get called with a state that still conflicts - Postgres raises the same exclusion violation
     * here it would otherwise raise at COMMIT, except the caller can actually catch it (a
     * {@code DataAccessException}, same as every other write in this service) and translate it
     * with {@code translateOverlap} instead of it surfacing as an unhandled failure the
     * transaction manager reports on its own.
     */
    @Modifying
    @Query(value = "SET CONSTRAINTS spa_appointment_no_table_overlap, spa_appointment_no_therapist_overlap IMMEDIATE", nativeQuery = true)
    void restoreImmediateOverlapConstraints();
}
