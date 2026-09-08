package com.sunsetbeach.entity;

import com.sunsetbeach.model.MaintenanceTaskStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** A reported problem in a physical room - see V35__maintenance_tasks.sql. */
@Entity
@Table(name = "MaintenanceTask")
public class MaintenanceTaskEntity {

    @Id
    @UuidGenerator
    private String id;

    private String roomUnitId;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MaintenanceTaskStatus status = MaintenanceTaskStatus.OPEN;

    // Optional - null until POST /maintenance-tasks/{id}/block links one. ON DELETE SET NULL at
    // the DB level (see the migration); the service still re-checks the block exists before
    // acting on it rather than trusting this field alone.
    private String blockId;

    private String reportedByUserId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] photos = new String[0];

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomUnitId() {
        return roomUnitId;
    }

    public void setRoomUnitId(String roomUnitId) {
        this.roomUnitId = roomUnitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenanceTaskStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceTaskStatus status) {
        this.status = status;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(String reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public String[] getPhotos() {
        return photos;
    }

    public void setPhotos(String[] photos) {
        this.photos = photos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
