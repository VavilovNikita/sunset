package com.sunsetbeach.entity;

import com.sunsetbeach.model.Weekday;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** What {@code POST /roster/generate} reads to seed a month - see V58's own comment. */
@Entity
@Table(name = "EmployeePattern")
public class EmployeePatternEntity {

    @Id
    private String employeeUserId;

    private String defaultShiftCodeId;

    private int workDaysPerWeek;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Weekday weeklyDayOff;

    private String updatedByUserId;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public String getDefaultShiftCodeId() {
        return defaultShiftCodeId;
    }

    public void setDefaultShiftCodeId(String defaultShiftCodeId) {
        this.defaultShiftCodeId = defaultShiftCodeId;
    }

    public int getWorkDaysPerWeek() {
        return workDaysPerWeek;
    }

    public void setWorkDaysPerWeek(int workDaysPerWeek) {
        this.workDaysPerWeek = workDaysPerWeek;
    }

    public Weekday getWeeklyDayOff() {
        return weeklyDayOff;
    }

    public void setWeeklyDayOff(Weekday weeklyDayOff) {
        this.weeklyDayOff = weeklyDayOff;
    }

    public String getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(String updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
