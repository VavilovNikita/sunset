package com.sunsetbeach.entity;

import com.sunsetbeach.model.FillColor;
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
 * Which actual {@code ShiftCode} code string a given {@code (rawCode, fillColor)} means - not
 * scoped by area. See V81__roster_import_color_mapping_global.sql's own comment for why: the
 * {@code ShiftCode} a code text resolves to is already area-scoped separately, at read time, by
 * {@code ShiftCodeService#resolveActive(staffArea, code)} - what this table remembers is only
 * which code text a colour means, and that's the same fact everywhere in this file, not one fact
 * per department. {@code resolvedCode} (not a {@code ShiftCode} id) is what's remembered, so a
 * later edit that retires and replaces that {@code ShiftCode} row (see V57's own versioning)
 * doesn't silently invalidate the mapping.
 */
@Entity
@Table(name = "RosterImportShiftColorMapping")
public class RosterImportShiftColorMappingEntity {

    @Id
    @UuidGenerator
    private String id;

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
