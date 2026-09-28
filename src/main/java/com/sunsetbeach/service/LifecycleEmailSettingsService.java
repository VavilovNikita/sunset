package com.sunsetbeach.service;

import com.sunsetbeach.entity.LifecycleEmailSettingsEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.repository.LifecycleEmailSettingsRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET/PUT /settings/lifecycle-emails} - the one row V112 seeds (see
 * {@link LifecycleEmailSettingsEntity}). ADMIN-only at the route level (SecurityConfig): this
 * decides what lands in every eligible guest's inbox.
 */
@Service
public class LifecycleEmailSettingsService {

    private final LifecycleEmailSettingsRepository repository;
    private final AuditLogService auditLogService;

    public LifecycleEmailSettingsService(LifecycleEmailSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public LifecycleEmailSettings get() {
        return toDto(getEntity());
    }

    /** The row always exists - V112 seeds it and nothing deletes it - so a missing one is a broken database, not a 404. */
    @Transactional(readOnly = true)
    public LifecycleEmailSettingsEntity getEntity() {
        return repository.findById(LifecycleEmailSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("LifecycleEmailSettings row is missing - V112 seeds it"));
    }

    @Transactional
    public LifecycleEmailSettings update(LifecycleEmailSettingsUpdateInput input) {
        String reviewUrl = input.getPostStayReviewUrl().isPresent() ? normalizeReviewUrl(input.getPostStayReviewUrl().get()) : null;

        LifecycleEmailSettingsEntity entity = getEntity();
        List<String> changes = new ArrayList<>();
        if (entity.isPreArrivalEnabled() != input.getPreArrivalEnabled()) {
            changes.add("pre-arrival " + (input.getPreArrivalEnabled() ? "enabled" : "disabled"));
        }
        if (entity.getPreArrivalDaysBefore() != input.getPreArrivalDaysBefore()) {
            changes.add("pre-arrival " + entity.getPreArrivalDaysBefore() + " -> " + input.getPreArrivalDaysBefore() + " day(s) before");
        }
        if (entity.isPostStayEnabled() != input.getPostStayEnabled()) {
            changes.add("post-stay " + (input.getPostStayEnabled() ? "enabled" : "disabled"));
        }
        if (entity.getPostStayDaysAfter() != input.getPostStayDaysAfter()) {
            changes.add("post-stay " + entity.getPostStayDaysAfter() + " -> " + input.getPostStayDaysAfter() + " day(s) after");
        }
        if (!Objects.equals(entity.getPostStayReviewUrl(), reviewUrl)) {
            changes.add(reviewUrl == null ? "review URL cleared" : "review URL set to " + reviewUrl);
        }
        if (entity.isWinBackEnabled() != input.getWinBackEnabled()) {
            changes.add("win-back " + (input.getWinBackEnabled() ? "enabled" : "disabled"));
        }
        if (entity.getWinBackMonthsSinceStay() != input.getWinBackMonthsSinceStay()) {
            changes.add("win-back " + entity.getWinBackMonthsSinceStay() + " -> " + input.getWinBackMonthsSinceStay() + " month(s) since stay");
        }

        entity.setPreArrivalEnabled(input.getPreArrivalEnabled());
        entity.setPreArrivalDaysBefore(input.getPreArrivalDaysBefore());
        entity.setPostStayEnabled(input.getPostStayEnabled());
        entity.setPostStayDaysAfter(input.getPostStayDaysAfter());
        entity.setPostStayReviewUrl(reviewUrl);
        entity.setWinBackEnabled(input.getWinBackEnabled());
        entity.setWinBackMonthsSinceStay(input.getWinBackMonthsSinceStay());
        LifecycleEmailSettingsEntity saved = repository.saveAndFlush(entity);

        // An unchanged PUT (the form saved as-is) records nothing - same as an unchanged
        // Booking.channel in BookingService#updateStatus.
        if (!changes.isEmpty()) {
            auditLogService.record(
                    AuditAction.LIFECYCLE_EMAIL_SETTINGS_UPDATED,
                    AuditEntityType.SETTINGS,
                    null,
                    "Lifecycle email settings: " + String.join("; ", changes));
        }
        return toDto(saved);
    }

    /**
     * Blank means none. Anything else must be an absolute http(s) URL: it goes straight into an
     * {@code href} in every post-stay email, so a {@code javascript:} or relative value is refused
     * here rather than shipped to guests.
     */
    private static String normalizeReviewUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) && uri.getHost() != null) {
                return trimmed;
            }
        } catch (java.net.URISyntaxException e) {
            // fall through
        }
        throw ValidationException.field("postStayReviewUrl", "Must be a full http:// or https:// link");
    }

    private static LifecycleEmailSettings toDto(LifecycleEmailSettingsEntity entity) {
        return new LifecycleEmailSettings(
                entity.isPreArrivalEnabled(),
                entity.getPreArrivalDaysBefore(),
                entity.isPostStayEnabled(),
                entity.getPostStayDaysAfter(),
                entity.getPostStayReviewUrl(),
                entity.isWinBackEnabled(),
                entity.getWinBackMonthsSinceStay(),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }
}
