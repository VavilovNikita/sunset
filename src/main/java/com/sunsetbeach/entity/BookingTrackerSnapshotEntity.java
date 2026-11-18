package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One Friday's frozen reading of one stay month (V161). Written only by {@code BookingTrackerSnapshotService}, by an
 * insert-if-absent on {@code (snapshotDate, stayMonth)}; the figures are never recomputed or edited. Only the four
 * notes change, through {@code PATCH /reports/booking-tracker/snapshots/{id}}.
 */
@Entity
@Table(name = "BookingTrackerSnapshot")
public class BookingTrackerSnapshotEntity {

    @Id
    private String id;

    private LocalDate snapshotDate;

    private String stayMonth;

    private boolean late;

    private int roomNights;

    private BigDecimal netRoomRevenue;

    private BigDecimal occupancyPercent;

    private String reason;

    private String nextAction;

    private String owner;

    private LocalDate dueDate;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public String getStayMonth() {
        return stayMonth;
    }

    public boolean isLate() {
        return late;
    }

    public int getRoomNights() {
        return roomNights;
    }

    public BigDecimal getNetRoomRevenue() {
        return netRoomRevenue;
    }

    public BigDecimal getOccupancyPercent() {
        return occupancyPercent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
