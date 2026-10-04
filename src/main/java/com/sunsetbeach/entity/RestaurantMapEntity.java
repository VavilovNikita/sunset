package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A single row, id always {@code "default"} - there is exactly one restaurant floor plan, not one per
 * table. See V124__restaurant_map.sql for why this is its own table rather than reusing {@link
 * SpaMapEntity}: replacing this image must never touch a Table's own position, and must
 * never be confused with replacing the spa map's or the property map's own image.
 */
@Entity
@Table(name = "RestaurantMap")
public class RestaurantMapEntity {

    @Id
    private String id = "default";

    private String imagePath;

    private String updatedByUserId;

    @UtcUpdateTimestamp
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
