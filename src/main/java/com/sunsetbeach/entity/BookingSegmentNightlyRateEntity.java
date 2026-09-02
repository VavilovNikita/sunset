package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One night's frozen price within a {@link BookingSegmentEntity} - see
 * V21__booking_segment_nightly_rates.sql's comment for why this exists (a segment's own
 * {@code totalPrice} used to be recomputed wholesale from current rates on every schedule
 * change, silently repricing nights the guest had already been quoted). Written once per night
 * (at insert, an extension, or a relocation's new segment - see {@code BookingWriter}'s class
 * javadoc) and never repriced except by the one explicit {@code POST /bookings/{id}/reprice}
 * action.
 *
 * <p>A row can outlive its segment's own {@code [checkIn, checkOut)} range: {@code
 * BookingWriter#relocate} deliberately leaves a truncated segment's rows for the nights it gave
 * up in place rather than deleting them, so {@code undoRelocation} can restore the guest's
 * original price for those nights later instead of charging whatever the room costs *then*. A
 * "stale-looking" row outside its segment's current dates is not dead data to clean up - it is
 * the only record of a price an undo may still need.
 */
@Entity
@Table(name = "BookingSegmentNightlyRate", uniqueConstraints = @UniqueConstraint(columnNames = {"segmentId", "date"}))
public class BookingSegmentNightlyRateEntity {

    @Id
    @UuidGenerator
    private String id;

    private String segmentId;

    private LocalDate date;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
