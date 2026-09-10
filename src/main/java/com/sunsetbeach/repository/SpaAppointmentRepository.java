package com.sunsetbeach.repository;

import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.model.SpaAppointmentStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaAppointmentRepository extends JpaRepository<SpaAppointmentEntity, String> {

    /** The grid's own query - everything on one date, regardless of status. */
    List<SpaAppointmentEntity> findByDate(LocalDate date);

    /**
     * Future, still-BOOKED appointments for one booking - used to warn (not block, see
     * CLAUDE.md's Failure handling section) when that booking is cancelled or shortened.
     */
    List<SpaAppointmentEntity> findByBookingIdAndStatusAndDateGreaterThanEqual(String bookingId, SpaAppointmentStatus status, LocalDate date);

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
}
