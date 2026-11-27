package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What the server remembers about one note of the staff manual's "What's new"; see V164 and
 * {@code ManualUpdateService}.
 */
@Entity
@Table(name = "ManualUpdateEntry")
public class ManualUpdateEntryEntity {

    /** Told to the directors; {@link #sentOn} is the hotel day the message is due. */
    public static final String SENT = "SENT";
    /** That day already had its message: goes with the next one. */
    public static final String HELD = "HELD";
    /** Seen while the switch was off: never sent. */
    public static final String SKIPPED = "SKIPPED";

    @Id
    private String entryId;

    private String state;

    private LocalDate sentOn;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }

    public void setSentOn(LocalDate sentOn) {
        this.sentOn = sentOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
