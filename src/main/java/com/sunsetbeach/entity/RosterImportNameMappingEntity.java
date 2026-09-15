package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Which account a name found in the Excel schedule import refers to - see
 * V78__roster_import_mappings.sql's own comment for why this is remembered at all, and {@code
 * RosterImportService}'s own javadoc for how a mapping is resolved.
 */
@Entity
@Table(name = "RosterImportNameMapping")
public class RosterImportNameMappingEntity {

    @Id
    @UuidGenerator
    private String id;

    private String rawName;

    private String employeeUserId;

    private String createdByUserId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
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
