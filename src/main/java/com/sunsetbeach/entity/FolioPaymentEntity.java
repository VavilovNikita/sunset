package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.sunsetbeach.model.FolioPaymentMethod;

/**
 * Money actually collected against a booking's folio - see V26__booking_folio_payments.sql's
 * comment for why this exists (a room-charge {@link PaymentEntity} could never be marked
 * collected before this) and why it's deliberately not a loosened {@code Payment.orderId} or
 * tied to a {@link ShiftEntity}. {@link com.sunsetbeach.service.BookingService}'s
 * {@code computeFolioBreakdown} is the one place these get summed against what was charged.
 */
@Entity
@Table(name = "FolioPayment")
public class FolioPaymentEntity {

    @Id
    @UuidGenerator
    private String id;

    private String bookingId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FolioPaymentMethod method;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private String recordedByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public FolioPaymentMethod getMethod() {
        return method;
    }

    public void setMethod(FolioPaymentMethod method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRecordedByUserId() {
        return recordedByUserId;
    }

    public void setRecordedByUserId(String recordedByUserId) {
        this.recordedByUserId = recordedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
