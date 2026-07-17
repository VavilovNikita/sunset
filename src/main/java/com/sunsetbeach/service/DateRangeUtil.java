package com.sunsetbeach.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Date-range helpers mirroring lib/bookings.ts. LocalDate has no time/zone component, so
 * unlike the JS version (which has to do all its math in Date.UTC to avoid local-timezone
 * calendar-day drift) there's no UTC pitfall to route around here.
 */
public final class DateRangeUtil {

    public static final int MAX_RANGE_DAYS = 366;

    private static final DateTimeFormatter ISO_INSTANT_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private DateRangeUtil() {
    }

    /** Matches Prisma DateTime's JSON serialization: always 3 fractional-second digits. */
    public static String formatIsoInstant(LocalDateTime utc) {
        return utc.format(ISO_INSTANT_MILLIS);
    }

    /** Inclusive list of calendar dates between {@code from} and {@code to}. */
    public static List<LocalDate> eachDateInRange(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }

    /** Nights of a stay: [checkIn, checkOut) - the checkout day itself isn't a night. */
    public static List<LocalDate> getNights(LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            nights.add(d);
        }
        return nights;
    }

    /** Parses a "YYYY-MM" month key, defaulting to the current UTC month. */
    public static YearMonth parseMonthOrCurrent(String monthParam) {
        if (monthParam == null || monthParam.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(monthParam);
        } catch (DateTimeParseException e) {
            return YearMonth.now(ZoneOffset.UTC);
        }
    }
}
