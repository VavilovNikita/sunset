package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * A WLED lighting controller on the hotel LAN - see {@code LightingService}. Which controllers exist
 * is runtime data an ADMIN enters, not a migration. {@code host} is only ever used through {@code
 * LanAddressGuard}.
 */
@Entity
@Table(name = "LightingDevice")
public class LightingDeviceEntity {

    @Id
    @UuidGenerator
    private String id;

    private String name;

    private String host;

    private int port = 80;

    private String location;

    private boolean active = true;

    private int sortOrder;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
