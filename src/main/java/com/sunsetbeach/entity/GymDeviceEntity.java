package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * A piece of gym equipment controlled through the Tuya cloud - see {@code GymDeviceService}. Which
 * devices exist is runtime data an ADMIN picks from the Tuya project, not a migration. For an
 * air conditioner the {@code lastAc*} columns hold the last command this server sent: the IR link
 * is one-way, so that is all that is known about its state.
 */
@Entity
@Table(name = "GymDevice")
public class GymDeviceEntity {

    @Id
    @UuidGenerator
    private String id;

    private String name;

    /** {@code RELAY} or {@code AC_IR} (a CHECK constraint, see V139). */
    private String type;

    private String tuyaDeviceId;

    private String infraredId;

    private String remoteId;

    private String switchCode;

    private int sortOrder;

    private boolean active = true;

    private Integer lastAcPower;

    private Integer lastAcMode;

    private Integer lastAcTemp;

    private Integer lastAcWind;

    /** UTC wall-clock like every timestamp column. */
    private LocalDateTime lastAcSentAt;

    /** Name of the staff member at the time - a snapshot, not a join. */
    private String lastAcSentBy;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTuyaDeviceId() { return tuyaDeviceId; }
    public void setTuyaDeviceId(String tuyaDeviceId) { this.tuyaDeviceId = tuyaDeviceId; }
    public String getInfraredId() { return infraredId; }
    public void setInfraredId(String infraredId) { this.infraredId = infraredId; }
    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }
    public String getSwitchCode() { return switchCode; }
    public void setSwitchCode(String switchCode) { this.switchCode = switchCode; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Integer getLastAcPower() { return lastAcPower; }
    public void setLastAcPower(Integer lastAcPower) { this.lastAcPower = lastAcPower; }
    public Integer getLastAcMode() { return lastAcMode; }
    public void setLastAcMode(Integer lastAcMode) { this.lastAcMode = lastAcMode; }
    public Integer getLastAcTemp() { return lastAcTemp; }
    public void setLastAcTemp(Integer lastAcTemp) { this.lastAcTemp = lastAcTemp; }
    public Integer getLastAcWind() { return lastAcWind; }
    public void setLastAcWind(Integer lastAcWind) { this.lastAcWind = lastAcWind; }
    public LocalDateTime getLastAcSentAt() { return lastAcSentAt; }
    public void setLastAcSentAt(LocalDateTime lastAcSentAt) { this.lastAcSentAt = lastAcSentAt; }
    public String getLastAcSentBy() { return lastAcSentBy; }
    public void setLastAcSentBy(String lastAcSentBy) { this.lastAcSentBy = lastAcSentBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
