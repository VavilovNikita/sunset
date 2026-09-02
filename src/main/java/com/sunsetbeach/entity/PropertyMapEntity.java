package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A single row, id always {@code "default"} - there is exactly one property-wide floor plan, not
 * one per room or room type. See V29__property_map.sql for why this is its own table rather than
 * a column on Room/RoomUnit: replacing this image must never touch a RoomUnit's own position.
 */
@Entity
@Table(name = "PropertyMap")
public class PropertyMapEntity {

    @Id
    private String id = "default";

    private String imagePath;

    private String updatedByUserId;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(String updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
