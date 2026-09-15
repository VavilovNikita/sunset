package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * A fingerprint terminal the server polls - see this entity's own openapi.yaml description for
 * the pull-vs-push security posture and what {@code lastSeenAt} is for.
 */
@Entity
@Table(name = "AttendanceDevice")
public class AttendanceDeviceEntity {

    @Id
    @UuidGenerator
    private String id;

    private String name;

    private String serial;

    private String address;

    private int port = 4370;

    private String timezone;

    private boolean active = true;

    private LocalDateTime lastSeenAt;

    /**
     * The ZK protocol's own per-record byte layout (16 or 40 - see {@code
     * ZkTerminalClientImpl#parseAttendanceRecords}), detected on this device's most recent full
     * log read and reused for every windowed read since - see that class's own javadoc for why a
     * windowed response can't redetect it the way a full one can. Null until the first full read
     * ever succeeds.
     */
    private Integer attendanceRecordSize;

    /**
     * Set from what the device actually answered on the most recent poll that attempted a
     * windowed read - never from a configuration guess - and cleared the moment a later poll
     * succeeds with one (firmware can be updated, or a replacement unit swapped in under the same
     * row). A poll that read the full log for an unrelated reason (no watermark yet, an empty
     * log) never touches this field either way - see {@code TerminalPollResult
     * #windowedReadUnsupported}'s own javadoc for why "didn't test it" has to stay distinct from
     * "tested it and it failed".
     */
    private boolean windowedReadUnsupported = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getAttendanceRecordSize() {
        return attendanceRecordSize;
    }

    public void setAttendanceRecordSize(Integer attendanceRecordSize) {
        this.attendanceRecordSize = attendanceRecordSize;
    }

    public boolean isWindowedReadUnsupported() {
        return windowedReadUnsupported;
    }

    public void setWindowedReadUnsupported(boolean windowedReadUnsupported) {
        this.windowedReadUnsupported = windowedReadUnsupported;
    }
}
