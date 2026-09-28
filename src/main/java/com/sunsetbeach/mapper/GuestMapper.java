package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.GuestEmailLogEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestAccountLinkSummary;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestEmailHistoryEntry;
import com.sunsetbeach.model.GuestUpdateInput;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    private final Clock clock;

    public GuestMapper(Clock clock) {
        this.clock = clock;
    }

    public Guest toDto(GuestEntity entity) {
        return new Guest(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getNotes(),
                entity.isVip(),
                entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null,
                Arrays.asList(entity.getTags()),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }

    /**
     * {@code bookings} must already be this guest's full stay history, newest first - see
     * {@code GuestDetail}'s own description. {@code account} is the linked self-service account,
     * or null; only its verification state is exposed, never credentials or tokens.
     */
    public GuestDetail toDetailDto(
            GuestEntity entity, List<Booking> bookings, GuestAccountEntity account, List<GuestEmailLogEntity> emailLog) {
        return new GuestDetail(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getNotes(),
                entity.isVip(),
                entity.getDateOfBirth() != null ? entity.getDateOfBirth().toString() : null,
                Arrays.asList(entity.getTags()),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()),
                bookings,
                account != null ? new GuestAccountLinkSummary(account.getEmailVerifiedAt() != null, account.isMarketingEmailsOptOut()) : null,
                emailLog.stream()
                        // sentAt is hotel wall-clock, not UTC - see GuestEmailLogEntity.
                        .map(e -> new GuestEmailHistoryEntry(
                                e.getType(), e.getSubject(), e.getSentAt().atZone(clock.getZone()).toOffsetDateTime(), e.getBookingId()))
                        .toList());
    }

    public void applyCreate(GuestEntity entity, GuestCreateInput input) {
        entity.setName(input.getName().trim());
        entity.setEmail(input.getEmail().isPresent() ? blankToNull(input.getEmail().get()) : null);
        entity.setPhone(input.getPhone().isPresent() ? blankToNull(input.getPhone().get()) : null);
        entity.setNotes(input.getNotes().isPresent() ? blankToNull(input.getNotes().get()) : null);
        entity.setVip(Boolean.TRUE.equals(input.getVip()));
        entity.setDateOfBirth(parseDateOfBirth(input.getDateOfBirth()));
        entity.setTags(normalizeTags(input.getTags()));
    }

    public void applyUpdate(GuestEntity entity, GuestUpdateInput input) {
        entity.setName(input.getName().trim());
        entity.setEmail(input.getEmail().isPresent() ? blankToNull(input.getEmail().get()) : null);
        entity.setPhone(input.getPhone().isPresent() ? blankToNull(input.getPhone().get()) : null);
        entity.setNotes(input.getNotes().isPresent() ? blankToNull(input.getNotes().get()) : null);
        entity.setVip(input.getVip());
        entity.setDateOfBirth(parseDateOfBirth(input.getDateOfBirth()));
        entity.setTags(normalizeTags(input.getTags()));
    }

    /**
     * The openapi {@code pattern} only guarantees the shape - {@code 1990-02-30} passes it, and an
     * unhandled {@link DateTimeParseException} would surface as a 500 - same reasoning as
     * {@code ReportDateRange#parseDate}. "Not in the future" needs the clock, so
     * {@code GuestService} checks that, not this mapper.
     */
    private static LocalDate parseDateOfBirth(JsonNullable<String> value) {
        String raw = value.isPresent() ? blankToNull(value.get()) : null;
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ValidationException.field("dateOfBirth", "Date of birth must be a valid date (YYYY-MM-DD)");
        }
    }

    /**
     * Trims each tag and silently drops blank ones and exact duplicates, keeping first-entered
     * order - so {@code ["", "vip", "  ", "vip"]} is stored as {@code ["vip"]}. Dropping, not
     * rejecting: a stray empty entry from a comma-separated input ("honeymoon, ") is a typing
     * artifact, not a mistake worth a 400. Near-duplicates ("VIP" vs "vip") are deliberately kept
     * as typed - no tag vocabulary is managed yet.
     */
    private static String[] normalizeTags(List<String> tags) {
        if (tags == null) {
            return new String[0];
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String trimmed = blankToNull(tag);
            if (trimmed != null) {
                normalized.add(trimmed);
            }
        }
        return normalized.toArray(String[]::new);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
