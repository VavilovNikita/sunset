package com.sunsetbeach.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Counts weekdays (Mon-Fri) strictly after {@code from}, up to and including {@code to} - i.e.
 * "how many business days has {@code from} been waiting, as of {@code to}". Used by
 * {@link BookingExpiryService} so a request submitted Friday evening doesn't have its
 * hold-and-review window silently eaten by a weekend nobody is working: two calendar days pass
 * either way, but only the weekdays in between count toward the auto-cancellation deadline.
 * No public holiday calendar - a genuine limitation, not an oversight; the hotel doesn't have one
 * to plug in yet.
 */
final class BusinessDayCounter {

    private BusinessDayCounter() {
    }

    static int countBusinessDaysBetween(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate day = from;
        while (day.isBefore(to)) {
            day = day.plusDays(1);
            DayOfWeek dayOfWeek = day.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }
}
