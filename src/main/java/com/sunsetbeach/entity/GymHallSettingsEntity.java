package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** The single row of gym hall settings - opening hours and automation parameters; seeded by V142, only ever updated. */
@Entity
@Table(name = "GymHallSettings")
public class GymHallSettingsEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    private LocalTime openTime;

    private LocalTime closeTime;

    private int leadMinutes;

    private int acMode;

    private int acTemp;

    private int acWind;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
    public int getLeadMinutes() { return leadMinutes; }
    public void setLeadMinutes(int leadMinutes) { this.leadMinutes = leadMinutes; }
    public int getAcMode() { return acMode; }
    public void setAcMode(int acMode) { this.acMode = acMode; }
    public int getAcTemp() { return acTemp; }
    public void setAcTemp(int acTemp) { this.acTemp = acTemp; }
    public int getAcWind() { return acWind; }
    public void setAcWind(int acWind) { this.acWind = acWind; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
