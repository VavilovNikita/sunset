package com.sunsetbeach.entity;

import com.sunsetbeach.model.StaffArea;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One version of one shift code in one {@link StaffArea} - see V57__staff_area_and_shift_code.sql
 * for why this is never updated once a {@code RosterEntry} references it, and why the code is
 * scoped to an area at all (the source spreadsheet's own "9" means a split shift in Restaurant/
 * Kitchen and a single shift in Front Office).
 */
@Entity
@Table(name = "ShiftCode")
public class ShiftCodeEntity {

    @Id
    @UuidGenerator
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StaffArea staffArea;

    private String code;

    private LocalTime startTime1;
    private LocalTime endTime1;
    private LocalTime startTime2;
    private LocalTime endTime2;

    private boolean countsAsWorked;
    private boolean isPaid;

    private java.time.LocalDate effectiveFrom;
    private boolean active = true;

    private String createdByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public StaffArea getStaffArea() {
        return staffArea;
    }

    public void setStaffArea(StaffArea staffArea) {
        this.staffArea = staffArea;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalTime getStartTime1() {
        return startTime1;
    }

    public void setStartTime1(LocalTime startTime1) {
        this.startTime1 = startTime1;
    }

    public LocalTime getEndTime1() {
        return endTime1;
    }

    public void setEndTime1(LocalTime endTime1) {
        this.endTime1 = endTime1;
    }

    public LocalTime getStartTime2() {
        return startTime2;
    }

    public void setStartTime2(LocalTime startTime2) {
        this.startTime2 = startTime2;
    }

    public LocalTime getEndTime2() {
        return endTime2;
    }

    public void setEndTime2(LocalTime endTime2) {
        this.endTime2 = endTime2;
    }

    public boolean isCountsAsWorked() {
        return countsAsWorked;
    }

    public void setCountsAsWorked(boolean countsAsWorked) {
        this.countsAsWorked = countsAsWorked;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public java.time.LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(java.time.LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
