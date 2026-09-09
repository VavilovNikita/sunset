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
     * Candidates for auto-linking a newly opened POS order to a treatment - see
     * {@link com.sunsetbeach.service.OrderService#create}. {@code BOOKED}/{@code COMPLETED} both
     * count (the order can legitimately be opened before or after the therapist marks the
     * treatment done); {@code orderId IS NULL} so an already-billed appointment is never a
     * candidate for a second order.
     */
    List<SpaAppointmentEntity> findByTableIdAndDateAndOrderIdIsNullAndStatusIn(
            String tableId, LocalDate date, List<SpaAppointmentStatus> statuses);
}
