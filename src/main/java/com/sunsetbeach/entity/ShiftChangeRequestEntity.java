package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * An employee's request to move or remove a shift; see V150. Status changes are made by conditional UPDATEs in the repository, never by saving this entity.
 */
@Entity
@Table(name = "ShiftChangeRequest")
public class ShiftChangeRequestEntity {

    @Id
    @UuidGenerator
    private String id;

    private String employeeUserId;

    private String rosterEntryId;

    private String type;

    private String status = "PENDING";

    private String channel;

    private String reason;

    private LocalDate shiftDate;

    private String shiftCodeId;

    private String shiftCode;

    private String shiftTimes;

    private LocalDate targetDate;

    private String decidedByUserId;

    private LocalDateTime decidedAt;

    private String decisionComment;

    private String expiredReason;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public String getRosterEntryId() {
        return rosterEntryId;
    }

    public void setRosterEntryId(String rosterEntryId) {
        this.rosterEntryId = rosterEntryId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getShiftCodeId() {
        return shiftCodeId;
    }

    public void setShiftCodeId(String shiftCodeId) {
        this.shiftCodeId = shiftCodeId;
    }

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }

    public String getShiftTimes() {
        return shiftTimes;
    }

    public void setShiftTimes(String shiftTimes) {
        this.shiftTimes = shiftTimes;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getDecidedByUserId() {
        return decidedByUserId;
    }

    public void setDecidedByUserId(String decidedByUserId) {
        this.decidedByUserId = decidedByUserId;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public void setDecisionComment(String decisionComment) {
        this.decisionComment = decisionComment;
    }

    public String getExpiredReason() {
        return expiredReason;
    }

    public void setExpiredReason(String expiredReason) {
        this.expiredReason = expiredReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
