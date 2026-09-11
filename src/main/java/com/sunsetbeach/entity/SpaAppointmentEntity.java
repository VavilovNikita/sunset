package com.sunsetbeach.entity;

import com.sunsetbeach.model.SpaAppointmentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * A half-hour-grid spa treatment slot - see V41__spa_appointment.sql for the double-booking
 * guard (a GiST exclusion constraint on each of tableId/therapistUserId, not enforced here).
 * {@code endTime} is deliberately not a field: it's always {@code startTime + durationMinutes},
 * computed wherever needed (including by the migration's own exclusion constraints), so
 * durationMinutes stays the single source of truth rather than two columns that could drift.
 * <p>
 * The treatments themselves live in {@link SpaAppointmentTreatmentEntity}, one row each - see
 * that entity's javadoc and V50__spa_appointment_treatment.sql. This row's own durationMinutes
 * is a maintained sum over those, never a second, independently-set number.
 */
@Entity
@Table(name = "SpaAppointment")
public class SpaAppointmentEntity {

    @Id
    @UuidGenerator
    private String id;

    private String bookingId;

    private String tableId;

    private String therapistUserId;

    private LocalDate date;

    private LocalTime startTime;

    /**
     * A maintained sum of {@link SpaAppointmentTreatmentEntity#getDurationMinutes()} over this
     * appointment's treatment rows, kept as a real column so the two GiST exclusion constraints
     * (V41/V43) keep reading one plain column off this row and never need to know the treatment
     * table exists.
     */
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SpaAppointmentStatus status = SpaAppointmentStatus.BOOKED;

    /** The POS order that charged this treatment, if any - see SpaAppointment's openapi.yaml description. */
    private String orderId;

    private String createdByUserId;

    private String cancelledByUserId;

    private String cancelReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTherapistUserId() {
        return therapistUserId;
    }

    public void setTherapistUserId(String therapistUserId) {
        this.therapistUserId = therapistUserId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SpaAppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(SpaAppointmentStatus status) {
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCancelledByUserId() {
        return cancelledByUserId;
    }

    public void setCancelledByUserId(String cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
