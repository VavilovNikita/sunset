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
import com.sunsetbeach.model.ShiftStatus;

@Entity
@Table(name = "Shift")
public class ShiftEntity {

    @Id
    @UuidGenerator
    private String id;

    private String openedByUserId;

    @CreationTimestamp
    private LocalDateTime openedAt;

    private String closedByUserId;

    private LocalDateTime closedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal openingCashFloat;

    @Column(precision = 10, scale = 2)
    private BigDecimal closingCashCounted;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ShiftStatus status = ShiftStatus.OPEN;

    private String notes;

    public String getId() {
        return id;
    }

    public String getOpenedByUserId() {
        return openedByUserId;
    }

    public void setOpenedByUserId(String openedByUserId) {
        this.openedByUserId = openedByUserId;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public String getClosedByUserId() {
        return closedByUserId;
    }

    public void setClosedByUserId(String closedByUserId) {
        this.closedByUserId = closedByUserId;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningCashFloat() {
        return openingCashFloat;
    }

    public void setOpeningCashFloat(BigDecimal openingCashFloat) {
        this.openingCashFloat = openingCashFloat;
    }

    public BigDecimal getClosingCashCounted() {
        return closingCashCounted;
    }

    public void setClosingCashCounted(BigDecimal closingCashCounted) {
        this.closingCashCounted = closingCashCounted;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
