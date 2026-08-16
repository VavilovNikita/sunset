package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "Availability", uniqueConstraints = @UniqueConstraint(columnNames = {"roomId", "date"}))
public class AvailabilityEntity {

    @Id
    @UuidGenerator
    private String id;

    private String roomId;

    private LocalDate date;

    /** How many units of the room type are manually pulled from sale on this date. Zero is equivalent to no row existing. */
    private int blockedCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
