package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * What the equipment automation did - or decided not to do - to one device for one phase (ON/OFF)
 * of one reservation. The automation's memory: it sends only what has no final row yet, so a
 * restart repeats nothing and a missed transition is picked up from here. See
 * {@code GymAutomationService}. Status and skip reason are CHECK-constrained text (V142).
 */
@Entity
@Table(name = "GymReservationAction")
public class GymReservationActionEntity {

    public static final String ON = "ON";
    public static final String OFF = "OFF";
    public static final String PENDING = "PENDING";
    public static final String OK = "OK";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";

    public static final String COVERED_BY_PREVIOUS = "COVERED_BY_PREVIOUS";
    public static final String NEXT_BOOKING_SOON = "NEXT_BOOKING_SOON";
    public static final String MISSED = "MISSED";
    public static final String CANCELLED = "CANCELLED";
    public static final String DEVICE_REMOVED = "DEVICE_REMOVED";

    @Id
    @UuidGenerator
    private String id;

    private String reservationId;

    /** Null once the device was removed from settings; {@link #deviceName} is the snapshot. */
    private String deviceId;

    private String deviceName;

    private String phase;

    private String status = PENDING;

    private String skipReason;

    private int attempts;

    private String lastError;

    /** UTC wall-clock. */
    private LocalDateTime lastAttemptAt;

    private LocalDateTime completedAt;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public boolean isPending() {
        return PENDING.equals(status);
    }

    /** The device was switched on (or was already on from the previous reservation) - what an OFF has to undo. */
    public boolean isEffectivelyOn() {
        return OK.equals(status) || FAILED.equals(status) || (SKIPPED.equals(status) && COVERED_BY_PREVIOUS.equals(skipReason));
    }

    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
