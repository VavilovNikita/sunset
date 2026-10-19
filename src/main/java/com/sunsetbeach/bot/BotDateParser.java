package com.sunsetbeach.bot;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a date a person typed to the bot: {@code DD.MM}, {@code DD.MM.YYYY}, {@code DD/MM}, {@code DD/MM/YYYY} or
 * {@code YYYY-MM-DD}. Pure - "today" is passed in (the hotel's, from the shared {@code Clock}), nothing here reads time.
 *
 * <ul>
 *   <li><b>Without a year</b> it is the nearest such date <em>not earlier than today</em>: "19.10" on 19 October is
 *       today, on 20 October it is next year. Of a 29 February the nearest leap year.</li>
 *   <li><b>A year above 2500 is Buddhist</b> (Thai staff write 2569 for 2026): 543 is taken off.</li>
 *   <li>A day that does not exist (31.02) or text that is not a date is empty - never a guess.</li>
 * </ul>
 * Whether the date is allowed (past, too far, busy) is not decided here; that is the shift-request rules' job.
 */
final class BotDateParser {

    private static final Pattern DAY_MONTH = Pattern.compile("^(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{4}))?$");
    private static final Pattern ISO = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
    private static final int BUDDHIST_FROM = 2500;
    private static final int BUDDHIST_OFFSET = 543;
    /** A 29 February is at most eight years away (the 1900/2100 gap). */
    private static final int LOOKAHEAD_YEARS = 8;

    private BotDateParser() {
    }

    static Optional<LocalDate> parse(String text, LocalDate today) {
        if (text == null) {
            return Optional.empty();
        }
        String s = text.strip();
        Matcher iso = ISO.matcher(s);
        if (iso.matches()) {
            return of(year(iso.group(1)), iso.group(2), iso.group(3));
        }
        Matcher dm = DAY_MONTH.matcher(s);
        if (!dm.matches()) {
            return Optional.empty();
        }
        if (dm.group(3) != null) {
            return of(year(dm.group(3)), dm.group(2), dm.group(1));
        }
        int day = Integer.parseInt(dm.group(1));
        int month = Integer.parseInt(dm.group(2));
        for (int y = today.getYear(); y <= today.getYear() + LOOKAHEAD_YEARS; y++) {
            try {
                LocalDate candidate = LocalDate.of(y, month, day);
                if (!candidate.isBefore(today)) {
                    return Optional.of(candidate);
                }
            } catch (DateTimeException e) {
                // 29 February of a common year, or 31.04 in every year: try the next year, then give up.
            }
        }
        return Optional.empty();
    }

    private static int year(String digits) {
        int y = Integer.parseInt(digits);
        return y > BUDDHIST_FROM ? y - BUDDHIST_OFFSET : y;
    }

    private static Optional<LocalDate> of(int year, String month, String day) {
        try {
            return Optional.of(LocalDate.of(year, Integer.parseInt(month), Integer.parseInt(day)));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
