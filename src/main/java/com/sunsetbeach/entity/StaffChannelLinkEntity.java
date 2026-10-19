package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/**
 * A person's account on a messaging channel; see V148.
 */
@Entity
@Table(name = "StaffChannelLink")
public class StaffChannelLinkEntity {

    @Id
    @UuidGenerator
    private String id;

    private String userId;

    private String channel;

    private String externalId;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    private String menuSignature;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMenuSignature() {
        return menuSignature;
    }

    public void setMenuSignature(String menuSignature) {
        this.menuSignature = menuSignature;
    }
}
