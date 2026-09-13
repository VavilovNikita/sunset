package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One version of an employee's daily rate - never edited, only superseded. See V66's own
 * comment: a raise partway through a month must price each day against whichever rate was
 * actually in effect that day.
 */
@Entity
@Table(name = "EmployeePayRate")
public class EmployeePayRateEntity {

    @Id
    @UuidGenerator
    private String id;

    private String employeeUserId;

    @jakarta.persistence.Column(precision = 10, scale = 2)
    private BigDecimal dailyRate;

    private LocalDate effectiveFrom;

    private String createdByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
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
