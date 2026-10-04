package com.sunsetbeach.entity;

import com.sunsetbeach.model.JournalSourceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One posted journal entry - its lines are {@link JournalLineEntity} rows. Written only by
 * {@link com.sunsetbeach.service.LedgerService}, which checks debits = credits before insert, and
 * never changed afterwards: a mistake is corrected by a reversing entry ({@code reversesEntryId}).
 */
@Entity
@Immutable
@Table(name = "JournalEntry")
public class JournalEntryEntity {

    @Id
    @UuidGenerator
    private String id;

    private LocalDate entryDate;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private JournalSourceType sourceType;

    private String sourceId;

    private String reversesEntryId;

    private String createdByUserId;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JournalSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(JournalSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getReversesEntryId() {
        return reversesEntryId;
    }

    public void setReversesEntryId(String reversesEntryId) {
        this.reversesEntryId = reversesEntryId;
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
