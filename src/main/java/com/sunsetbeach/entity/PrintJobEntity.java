package com.sunsetbeach.entity;

import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "PrintJob")
public class PrintJobEntity {

    @Id
    @UuidGenerator
    private String id;

    private String printerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PrintDocumentType documentType;

    private String summary;

    /** Raw ESC/POS bytes, built once at enqueue time - a retry replays exactly this, never re-renders the document. */
    private byte[] payload;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PrintJobStatus status = PrintJobStatus.PENDING;

    private int attempts;

    private String lastError;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Closes a FAILED job as not-actionable without deleting it - see V31__print_job_dismiss.sql.
    // Null dismissedAt is "not dismissed", the default; all three are set together, never alone.
    private LocalDateTime dismissedAt;

    private String dismissedByUserId;

    private String dismissNote;

    public String getId() {
        return id;
    }

    public String getPrinterId() {
        return printerId;
    }

    public void setPrinterId(String printerId) {
        this.printerId = printerId;
    }

    public PrintDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(PrintDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public PrintJobStatus getStatus() {
        return status;
    }

    public void setStatus(PrintJobStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(LocalDateTime dismissedAt) {
        this.dismissedAt = dismissedAt;
    }

    public String getDismissedByUserId() {
        return dismissedByUserId;
    }

    public void setDismissedByUserId(String dismissedByUserId) {
        this.dismissedByUserId = dismissedByUserId;
    }

    public String getDismissNote() {
        return dismissNote;
    }

    public void setDismissNote(String dismissNote) {
        this.dismissNote = dismissNote;
    }
}
