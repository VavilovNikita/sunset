package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The single row holding the service-charge rate - see V161's own comment. Its id is always
 * {@link #SINGLETON_ID}; the row is seeded by that migration and only ever updated, never created
 * or deleted by the app. Same shape as {@link LifecycleEmailSettingsEntity}.
 */
@Entity
@Table(name = "ServiceChargeSettings")
public class ServiceChargeSettingsEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private BigDecimal serviceChargeRate;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public BigDecimal getServiceChargeRate() {
        return serviceChargeRate;
    }

    public void setServiceChargeRate(BigDecimal serviceChargeRate) {
        this.serviceChargeRate = serviceChargeRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
