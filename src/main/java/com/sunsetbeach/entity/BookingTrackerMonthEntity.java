package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What a manager typed for one stay month of the booking tracker - target room nights, baseline room nights and
 * baseline net revenue (V161). A row exists only while at least one of the three is set.
 */
@Entity
@Table(name = "BookingTrackerMonth")
public class BookingTrackerMonthEntity {

    /** {@code YYYY-MM}. */
    @Id
    private String month;

    private Integer targetRoomNights;

    private Integer baselineRoomNights;

    private BigDecimal baselineNetRevenue;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    protected BookingTrackerMonthEntity() {}

    public BookingTrackerMonthEntity(String month) {
        this.month = month;
    }

    public String getMonth() {
        return month;
    }

    public Integer getTargetRoomNights() {
        return targetRoomNights;
    }

    public void setTargetRoomNights(Integer targetRoomNights) {
        this.targetRoomNights = targetRoomNights;
    }

    public Integer getBaselineRoomNights() {
        return baselineRoomNights;
    }

    public void setBaselineRoomNights(Integer baselineRoomNights) {
        this.baselineRoomNights = baselineRoomNights;
    }

    public BigDecimal getBaselineNetRevenue() {
        return baselineNetRevenue;
    }

    public void setBaselineNetRevenue(BigDecimal baselineNetRevenue) {
        this.baselineNetRevenue = baselineNetRevenue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
