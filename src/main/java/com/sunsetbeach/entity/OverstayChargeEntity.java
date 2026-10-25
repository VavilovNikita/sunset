package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * One night a checked-in guest stayed past the booking's {@code checkOut}, charged to the booking - see
 * V158__overstay_charge.sql and {@code OverstayChargeService}. Unique per (booking, night); {@code voidedAt}
 * set means the night is not charged any more (and its ledger entry has been reversed).
 */
@Entity
@Table(name = "OverstayCharge", uniqueConstraints = @UniqueConstraint(columnNames = {"bookingId", "date"}))
public class OverstayChargeEntity {

    @Id
    @UuidGenerator
    private String id;

    private String bookingId;

    private LocalDate date;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String entryId;

    private LocalDateTime voidedAt;

    private String voidEntryId;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidEntryId() { return voidEntryId; }
    public void setVoidEntryId(String voidEntryId) { this.voidEntryId = voidEntryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return voidedAt == null; }
}
