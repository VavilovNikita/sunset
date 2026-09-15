package com.sunsetbeach.entity;

import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.PunchSource;
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

    @CreationTimestamp
    private LocalDateTime createdAt;

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
}
