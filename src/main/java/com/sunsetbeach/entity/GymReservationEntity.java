package com.sunsetbeach.entity;

import com.sunsetbeach.model.GymReservationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * A booking of the (single, exclusive) gym for a half-hour-grid slot - see V142 for the exclusion
 * constraint that keeps two reservations from overlapping. {@code date}/{@code startTime} are
 * hotel wall-clock; the end is always {@code startTime + durationMinutes}, never a column. What
 * the equipment automation did for the reservation lives in {@link GymReservationActionEntity}.
 */
@Entity
@Table(name = "GymReservation")
public class GymReservationEntity {

    @Id
    @UuidGenerator
    private String id;

    private String bookingId;

    private LocalDate date;

    private LocalTime startTime;

    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GymReservationStatus status = GymReservationStatus.BOOKED;

    /** Exactly one of these two is set (a CHECK in V142). */
    private String createdByUserId;

    private String createdByGuestAccountId;

    /** UTC wall-clock like every timestamp column. */
    private LocalDateTime cancelledAt;

    private String cancelledBy;

    private String cancelReason;

    private boolean automationDone;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public LocalDateTime start() {
        return date.atTime(startTime);
    }

    public LocalDateTime end() {
        return start().plusMinutes(durationMinutes);
    }

    public String getId() { return id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public GymReservationStatus getStatus() { return status; }
    public void setStatus(GymReservationStatus status) { this.status = status; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getCreatedByGuestAccountId() { return createdByGuestAccountId; }
    public void setCreatedByGuestAccountId(String createdByGuestAccountId) { this.createdByGuestAccountId = createdByGuestAccountId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public boolean isAutomationDone() { return automationDone; }
    public void setAutomationDone(boolean automationDone) { this.automationDone = automationDone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
