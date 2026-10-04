package com.sunsetbeach.entity;

import com.sunsetbeach.model.LifecycleEmailType;
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
 * One automated lifecycle email that actually went out - see V111's own comment and
 * {@code LifecycleEmailService}. Append-only: written once after a successful send, never
 * updated.
 */
@Entity
@Table(name = "GuestEmailLog")
public class GuestEmailLogEntity {

    @Id
    @UuidGenerator
    private String id;

    private String guestId;

    // Null for WIN_BACK - it isn't about one stay.
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LifecycleEmailType type;

    private String subject;

    // Hotel-local (Asia/Bangkok) wall-clock, set by LifecycleEmailService from the shared Clock -
    // deliberately not @UtcCreationTimestamp. The win-back window compares this against a
    // Clock-derived cutoff, and the two must come from the same time source (see CLAUDE.md,
    // "Clock and time zones"). For the same reason it's zoned with the Clock's zone on the way
    // out (GuestMapper), never TimestampFormat.toUtc.
    private LocalDateTime sentAt;

    public String getId() {
        return id;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public LifecycleEmailType getType() {
        return type;
    }

    public void setType(LifecycleEmailType type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
