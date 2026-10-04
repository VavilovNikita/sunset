package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * One hotel-local date that a person reviewed and closed - see V114's own comment. Append-only:
 * written once by {@code POST /night-audit/close}, never updated or deleted.
 */
@Entity
@Table(name = "NightAudit")
public class NightAuditEntity {

    @Id
    @UuidGenerator
    private String id;

    private LocalDate date;

    private String closedByUserId;

    @UtcCreationTimestamp
    private LocalDateTime closedAt;

    private String notes;

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getClosedByUserId() {
        return closedByUserId;
    }

    public void setClosedByUserId(String closedByUserId) {
        this.closedByUserId = closedByUserId;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
