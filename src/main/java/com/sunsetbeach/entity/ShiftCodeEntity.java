package com.sunsetbeach.entity;

import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One version of one shift code, optionally scoped to one {@link StaffArea} - see
 * V57__staff_area_and_shift_code.sql for why this is never updated once a {@code RosterEntry}
 * references it. V57's and V69's own comments explain area-scoping by "9" meaning a split shift
 * in Restaurant/Kitchen and a single shift in Front Office - the hotel's accountant has since
 * confirmed that's wrong: every code applies the same everywhere, and what the source spreadsheet
 * actually used to tell the two "9"s apart was cell fill colour, not department. See {@code
 * ShiftCode}'s own openapi.yaml description for the two distinct codes ("9" and "9S") this hotel
 * now uses instead of one ambiguous "9". Area-scoping itself stays exactly as built - correct,
 * tested, and cheaper to keep than to remove - even though nothing currently relies on it.
 */
@Entity
@Table(name = "ShiftCode")
public class ShiftCodeEntity {

    @Id
    @UuidGenerator
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StaffArea staffArea;

    private String code;

    private LocalTime startTime1;
    private LocalTime endTime1;
    private LocalTime startTime2;
    private LocalTime endTime2;

    private boolean countsAsWorked;
    private boolean isPaid;

    private java.time.LocalDate effectiveFrom;
    private boolean active = true;

    // Null only for a row that predates this column, not yet confirmed - see
    // ShiftCodeService#updateKind. Unlike every other field here, this one IS mutated in place on
    // an existing row: it classifies what the code already is, not an agreed term a RosterEntry
    // depends on staying frozen (see this class's own javadoc on why the rest never changes).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ShiftCodeKind kind;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalTime getStartTime1() {
        return startTime1;
    }

    public void setStartTime1(LocalTime startTime1) {
        this.startTime1 = startTime1;
    }

    public LocalTime getEndTime1() {
        return endTime1;
    }

    public void setEndTime1(LocalTime endTime1) {
        this.endTime1 = endTime1;
    }

    public LocalTime getStartTime2() {
        return startTime2;
    }

    public void setStartTime2(LocalTime startTime2) {
        this.startTime2 = startTime2;
    }

    public LocalTime getEndTime2() {
        return endTime2;
    }

    public void setEndTime2(LocalTime endTime2) {
        this.endTime2 = endTime2;
    }

    public boolean isCountsAsWorked() {
        return countsAsWorked;
    }

    public void setCountsAsWorked(boolean countsAsWorked) {
        this.countsAsWorked = countsAsWorked;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public java.time.LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(java.time.LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ShiftCodeKind getKind() {
        return kind;
    }

    public void setKind(ShiftCodeKind kind) {
        this.kind = kind;
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
