package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * An attendance terminal that is currently silent; see V148.
 */
@Entity
@Table(name = "TerminalSilenceEpisode")
public class TerminalSilenceEpisodeEntity {

    @Id
    private String deviceId;

    private LocalDateTime anchorLastSeenAt;

    private LocalDateTime startedAt;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getAnchorLastSeenAt() {
        return anchorLastSeenAt;
    }

    public void setAnchorLastSeenAt(LocalDateTime anchorLastSeenAt) {
        this.anchorLastSeenAt = anchorLastSeenAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

}
