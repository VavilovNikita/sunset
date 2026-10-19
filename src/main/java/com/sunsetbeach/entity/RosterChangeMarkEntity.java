package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * An employee-day whose shift a manager changed and whose announcement is waiting for the changes to stop; see V150.
 */
@Entity
@Table(name = "RosterChangeMark")
public class RosterChangeMarkEntity {

    @Id
    @UuidGenerator
    private String id;

    private String employeeUserId;

    private LocalDate shiftDate;

    private String beforeShiftCodeId;

    private LocalDateTime firstChangedAt;

    private LocalDateTime lastChangedAt;

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

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }

    public String getBeforeShiftCodeId() {
        return beforeShiftCodeId;
    }

    public void setBeforeShiftCodeId(String beforeShiftCodeId) {
        this.beforeShiftCodeId = beforeShiftCodeId;
    }

    public LocalDateTime getFirstChangedAt() {
        return firstChangedAt;
    }

    public void setFirstChangedAt(LocalDateTime firstChangedAt) {
        this.firstChangedAt = firstChangedAt;
    }

    public LocalDateTime getLastChangedAt() {
        return lastChangedAt;
    }

    public void setLastChangedAt(LocalDateTime lastChangedAt) {
        this.lastChangedAt = lastChangedAt;
    }
}
