package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The single row holding the VAT rate - see V118's own comment. Its id is always
 * {@link #SINGLETON_ID}; the row is seeded by that migration and only ever updated, never created
 * or deleted by the app. Same shape as {@link LifecycleEmailSettingsEntity}.
 */
@Entity
@Table(name = "VatSettings")
public class VatSettingsEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private BigDecimal vatRate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
