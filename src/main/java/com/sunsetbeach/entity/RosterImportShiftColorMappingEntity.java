package com.sunsetbeach.entity;

import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
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

/**
 * Which actual {@code ShiftCode} a given {@code (staffArea, rawCode, fillColor)} means - see
 * V78__roster_import_mappings.sql's own comment for why a code string, not a specific {@code
 * ShiftCode} id, is what gets remembered.
 */
@Entity
@Table(name = "RosterImportShiftColorMapping")
public class RosterImportShiftColorMappingEntity {

    @Id
    @UuidGenerator
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StaffArea staffArea;

    private String rawCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FillColor fillColor;

    private String resolvedCode;

    private String createdByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public StaffArea getStaffArea() {
        return staffArea;
    }

    public void setStaffArea(StaffArea staffArea) {
        this.staffArea = staffArea;
    }

    public String getRawCode() {
        return rawCode;
    }

    public void setRawCode(String rawCode) {
        this.rawCode = rawCode;
    }

    public FillColor getFillColor() {
        return fillColor;
    }

    public void setFillColor(FillColor fillColor) {
        this.fillColor = fillColor;
    }

    public String getResolvedCode() {
        return resolvedCode;
    }

    public void setResolvedCode(String resolvedCode) {
        this.resolvedCode = resolvedCode;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
