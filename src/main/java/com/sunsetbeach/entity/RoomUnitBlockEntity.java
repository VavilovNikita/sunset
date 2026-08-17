package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/** A physical room pulled off sale for [fromDate, toDate] (inclusive), with a reason. Replaces the old Room-level Availability.blockedCount. */
@Entity
@Table(name = "RoomUnitBlock")
public class RoomUnitBlockEntity {

    @Id
    @UuidGenerator
    private String id;

    private String roomUnitId;

    private LocalDate fromDate;

    private LocalDate toDate;

    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomUnitId() {
        return roomUnitId;
    }

    public void setRoomUnitId(String roomUnitId) {
        this.roomUnitId = roomUnitId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
