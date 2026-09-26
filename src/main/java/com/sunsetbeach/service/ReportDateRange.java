package com.sunsetbeach.service;

import com.sunsetbeach.error.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * The inclusive {@code from}/{@code to} hotel-local date range every {@code /reports/*} endpoint
 * takes - one parser, so a range means the same thing (and fails the same way) on every report.
 */
record ReportDateRange(LocalDate from, LocalDate to) {

    static ReportDateRange parse(String from, String to) {
        LocalDate fromDate = parseDate("from", from);
        LocalDate toDate = parseDate("to", to);
        if (fromDate.isAfter(toDate)) {
            throw ValidationException.field("to", "must be on or after from");
        }
        return new ReportDateRange(fromDate, toDate);
    }

    /**
     * The openapi {@code pattern} only guarantees the shape - {@code 2026-02-30} passes it, and
     * an unhandled {@link DateTimeParseException} would surface as a 500, not the documented 400.
     */
    private static LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.field(field, "must be a valid date (YYYY-MM-DD)");
        }
    }

    /** Number of calendar days in the range, both ends included - also its number of nights. */
    long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    /**
     * Local midnight at the start of {@code from}, as the UTC wall-clock a {@code @CreationTimestamp}
     * column stores - compare with {@code >=}.
     */
    LocalDateTime startUtc(ZoneId zone) {
        return from.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Local midnight after {@code to}, as UTC wall-clock - compare with {@code <}. */
    LocalDateTime endUtcExclusive(ZoneId zone) {
        return to.plusDays(1).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
