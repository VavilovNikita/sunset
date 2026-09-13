package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One employee's shift on one date - a day off is the absence of a row, never a row of its own.
 * See V60__roster_entry.sql for why {@code UNIQUE(employeeUserId, date)} is the concurrency
 * primitive the whole editing surface (move/reassign/swap) depends on.
 */
@Entity
@Table(name = "RosterEntry")
public class RosterEntryEntity {

    @Id
    @UuidGenerator
    private String id;

    private String employeeUserId;

    private LocalDate date;

    private String shiftCodeId;

    private String note;

    private boolean locked = false;

    private String createdByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getShiftCodeId() {
        return shiftCodeId;
    }

    public void setShiftCodeId(String shiftCodeId) {
        this.shiftCodeId = shiftCodeId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
