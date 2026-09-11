package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * One treatment on a {@link SpaAppointmentEntity} - see that entity's own javadoc for why this
 * table exists (a child row per treatment, no quantity field) and V50__spa_appointment_treatment.sql
 * for why it carries no price. {@code durationMinutes} is frozen at add time from
 * {@code MenuItem.durationMinutes}; the parent appointment's own {@code durationMinutes} is a
 * maintained sum over these rows, kept so the two GiST exclusion constraints (V41/V43) keep
 * reading one plain column and never need to know this table exists.
 */
@Entity
@Table(name = "SpaAppointmentTreatment")
public class SpaAppointmentTreatmentEntity {

    @Id
    @UuidGenerator
    private String id;

    private String spaAppointmentId;

    private String treatmentMenuItemId;

    private int durationMinutes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getSpaAppointmentId() {
        return spaAppointmentId;
    }

    public void setSpaAppointmentId(String spaAppointmentId) {
        this.spaAppointmentId = spaAppointmentId;
    }

    public String getTreatmentMenuItemId() {
        return treatmentMenuItemId;
    }

    public void setTreatmentMenuItemId(String treatmentMenuItemId) {
        this.treatmentMenuItemId = treatmentMenuItemId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
