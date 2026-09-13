package com.sunsetbeach.entity;

import com.sunsetbeach.model.StaffArea;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** One minimum-staffing rule per area - an area with no row here has no minimum and never warns. */
@Entity
@Table(name = "StaffAreaCoverageRule")
public class StaffAreaCoverageRuleEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StaffArea staffArea;

    private int minimumWorking;

    private String updatedByUserId;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public StaffArea getStaffArea() {
        return staffArea;
    }

    public void setStaffArea(StaffArea staffArea) {
        this.staffArea = staffArea;
    }

    public int getMinimumWorking() {
        return minimumWorking;
    }

    public void setMinimumWorking(int minimumWorking) {
        this.minimumWorking = minimumWorking;
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
