package com.sunsetbeach.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * A date for people to read in an audit entry: {@code 19 Nov 2026}, never {@code 2026-11-19}. The
 * companion of {@link PriceFormat#baht} - display text only; API dates stay {@code YYYY-MM-DD}.
 */
public final class DateText {

    private static final DateTimeFormatter READABLE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private DateText() {
    }

    public static String date(LocalDate value) {
        return value == null ? "-" : READABLE.format(value);
    }
}
