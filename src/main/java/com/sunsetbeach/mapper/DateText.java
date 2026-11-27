package com.sunsetbeach.mapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * A date for people to read - an audit entry, a ledger description, an error or warning message:
 * {@code 19 Nov 2026}, never {@code 2026-11-19}. The companion of {@link PriceFormat#baht} -
 * display text only; API dates, keys and file names stay {@code YYYY-MM-DD}.
 */
public final class DateText {

    private static final DateTimeFormatter READABLE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter READABLE_MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private DateText() {
    }

    public static String date(LocalDate value) {
        return value == null ? "-" : READABLE.format(value);
    }

    /** {@link #date} for a value already held as the API's {@code YYYY-MM-DD} string. */
    public static String iso(String value) {
        return value == null ? "-" : date(LocalDate.parse(value));
    }

    /** {@code Nov 2026}. */
    public static String month(YearMonth value) {
        return value == null ? "-" : READABLE_MONTH.format(value);
    }

    /** {@link #month} for a stored {@code YYYY-MM} string. */
    public static String isoMonth(String value) {
        return value == null ? "-" : month(YearMonth.parse(value));
    }
}
