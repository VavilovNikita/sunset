package com.sunsetbeach.entity;

import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One raw clock-in or clock-out - not a paired session. See V64's own comment: pairing into
 * worked intervals happens at read time, never here, so a split shift's four punches are simply
 * four rows and a missed punch is left as what it is (an odd count) rather than guessed at.
 */
@Entity
@Table(name = "AttendancePunch")
public class AttendancePunchEntity {

    @Id
    @UuidGenerator
    private String id;

    private String employeeUserId;

    private LocalDateTime punchAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PunchDirection direction;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PunchSource source;

    private String recordedByUserId;

    private String note;

    // Both null together for MANUAL, both set together for SCANNER (see V75's own CHECK
    // constraint). enrollmentNumber is the device's own reported number, kept alongside the
    // resolved employeeUserId above rather than only implied by it - see the ingestion
    // idempotency key this pair (with deviceId and punchAt) forms.
    private String deviceId;

    private Integer enrollmentNumber;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    // Set on the punches a PUT /attendance/day correction recorded.
    private String correctionId;

    // Void columns: all set or all null (V134). A voided punch is kept as history and counted
    // nowhere. voidedAt is UTC wall-clock like every timestamp column (TimestampFormat.nowUtc).
    private LocalDateTime voidedAt;

    private String voidedByUserId;

    private String voidedByCorrectionId;

    private String voidReason;

    public String getId() {
        return id;
    }

    public String getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public LocalDateTime getPunchAt() {
        return punchAt;
    }

    public void setPunchAt(LocalDateTime punchAt) {
        this.punchAt = punchAt;
    }

    public PunchDirection getDirection() {
        return direction;
    }

    public void setDirection(PunchDirection direction) {
        this.direction = direction;
    }

    public PunchSource getSource() {
        return source;
    }

    public void setSource(PunchSource source) {
        this.source = source;
    }

    public String getRecordedByUserId() {
        return recordedByUserId;
    }

    public void setRecordedByUserId(String recordedByUserId) {
        this.recordedByUserId = recordedByUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getEnrollmentNumber() {
        return enrollmentNumber;
    }

    public void setEnrollmentNumber(Integer enrollmentNumber) {
        this.enrollmentNumber = enrollmentNumber;
    }

    public String getCorrectionId() {
        return correctionId;
    }

    public void setCorrectionId(String correctionId) {
        this.correctionId = correctionId;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }

    public String getVoidedByUserId() {
        return voidedByUserId;
    }

    public String getVoidedByCorrectionId() {
        return voidedByCorrectionId;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    /** The only edit a punch ever gets: marked replaced, with who/when/why. Never un-voided. */
    public void voidBy(String userId, String correctionId, String reason, LocalDateTime at) {
        this.voidedAt = at;
        this.voidedByUserId = userId;
        this.voidedByCorrectionId = correctionId;
        this.voidReason = reason;
    }
}
