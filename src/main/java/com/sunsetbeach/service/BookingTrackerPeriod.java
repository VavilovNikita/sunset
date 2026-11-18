package com.sunsetbeach.service;

import com.sunsetbeach.error.ValidationException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The stay months a booking-tracker request covers: an inclusive range of whole months (at most
 * {@value #MAX_MONTHS}) and an optional single month the channel and trends sections are narrowed to. Pure - the
 * default is derived from the date handed in (hotel "today" at the call site), never from a clock read here.
 */
record BookingTrackerPeriod(YearMonth from, YearMonth to, YearMonth stayMonth) {

    static final int MAX_MONTHS = 12;

    /**
     * Parses the three query parameters. Both range ends omitted: the season that contains {@code today}, or the
     * coming one while today is May to October ({@link #seasonFor}). One end alone is a 400 - there is no honest
     * default for the other.
     */
    static BookingTrackerPeriod parse(String fromMonth, String toMonth, String stayMonth, LocalDate today) {
        YearMonth from;
        YearMonth to;
        if (fromMonth == null && toMonth == null) {
            BookingTrackerPeriod season = seasonFor(today);
            from = season.from();
            to = season.to();
        } else if (fromMonth == null || toMonth == null) {
            throw ValidationException.field(fromMonth == null ? "fromMonth" : "toMonth", "fromMonth and toMonth go together");
        } else {
            from = parseMonth("fromMonth", fromMonth);
            to = parseMonth("toMonth", toMonth);
        }
        if (to.isBefore(from)) {
            throw ValidationException.field("toMonth", "must be on or after fromMonth");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_MONTHS) {
            throw ValidationException.field("toMonth", "at most " + MAX_MONTHS + " months");
        }
        YearMonth stay = stayMonth == null ? null : parseMonth("stayMonth", stayMonth);
        if (stay != null && (stay.isBefore(from) || stay.isAfter(to))) {
            throw ValidationException.field("stayMonth", "must lie inside the range");
        }
        return new BookingTrackerPeriod(from, to, stay);
    }

    /**
     * The hotel's season, November to April: the one that contains {@code today}, or - May to October, when none
     * does - the one that starts next.
     */
    static BookingTrackerPeriod seasonFor(LocalDate today) {
        int year = today.getYear();
        int month = today.getMonthValue();
        YearMonth start = month <= 4 ? YearMonth.of(year - 1, 11) : YearMonth.of(year, 11);
        return new BookingTrackerPeriod(start, start.plusMonths(5), null);
    }

    /** The season after this one. */
    BookingTrackerPeriod nextSeason() {
        return new BookingTrackerPeriod(from.plusMonths(12), to.plusMonths(12), null);
    }

    List<YearMonth> months() {
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            months.add(m);
        }
        return months;
    }

    /** The months the channel and trends sections look at: the stay month, or all of them. */
    List<YearMonth> filterMonths() {
        return stayMonth != null ? List.of(stayMonth) : months();
    }

    LocalDate firstDay() {
        return from.atDay(1);
    }

    LocalDate lastDay() {
        return to.atEndOfMonth();
    }

    static YearMonth parseMonth(String field, String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.field(field, "must be a month (YYYY-MM)");
        }
    }
}
