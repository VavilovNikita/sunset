package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * A quantity voided off a sent order line - see V128 and {@code voidOrderItem}'s openapi.yaml
 * description. Written once by {@code OrderService#voidItem}, never updated.
 */
@Entity
@Table(name = "OrderItemVoid")
public class OrderItemVoidEntity {

    @Id
    @UuidGenerator
    private String id;

    private String orderId;

    private String menuItemId;

    private int quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private String note;

    private LocalDateTime sentAt;

    private String reason;

    private String voidedByUserId;

    @CreationTimestamp
    private LocalDateTime voidedAt;

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getVoidedByUserId() {
        return voidedByUserId;
    }

    public void setVoidedByUserId(String voidedByUserId) {
        this.voidedByUserId = voidedByUserId;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }
}
