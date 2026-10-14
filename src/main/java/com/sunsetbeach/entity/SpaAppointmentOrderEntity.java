package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A further order that bills a {@link SpaAppointmentEntity} - see V144. The appointment's own,
 * first order stays in {@code SpaAppointment.orderId}; this row exists for the top-up order opened
 * when that one is already paid. An order is linked to one appointment at most.
 */
@Entity
@Table(name = "SpaAppointmentOrder")
public class SpaAppointmentOrderEntity {

    @Id
    private String orderId;

    private String spaAppointmentId;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public SpaAppointmentOrderEntity() {
    }

    public SpaAppointmentOrderEntity(String orderId, String spaAppointmentId) {
        this.orderId = orderId;
        this.spaAppointmentId = spaAppointmentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSpaAppointmentId() {
        return spaAppointmentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
