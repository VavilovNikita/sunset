package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

/** SiteMinder's room type name -> this system's room type - see V123__siteminder_import.sql. */
@Entity
@Table(name = "SiteMinderRoomTypeMapping")
public class SiteMinderRoomTypeMappingEntity {

    @Id
    @UuidGenerator
    private String id;

    private String siteMinderRoomType;

    private String roomId;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public String getSiteMinderRoomType() {
        return siteMinderRoomType;
    }

    public void setSiteMinderRoomType(String siteMinderRoomType) {
        this.siteMinderRoomType = siteMinderRoomType;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
