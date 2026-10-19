package com.sunsetbeach.service;

import com.sunsetbeach.entity.ShiftCodeEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** How a shift code reads in a message, and when a shift starts - one place for the bot, the requests and the reminders. */
public final class ShiftLabels {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private ShiftLabels() {
    }

    /** {@code 09:00-18:00} or {@code 09:00-13:00, 17:00-21:00}; null for a code with no fixed times (OP, a day-off kind). */
    public static String times(ShiftCodeEntity code) {
        if (code == null || code.getStartTime1() == null || code.getEndTime1() == null) {
            return null;
        }
        String first = code.getStartTime1().format(HHMM) + "-" + code.getEndTime1().format(HHMM);
        if (code.getStartTime2() == null || code.getEndTime2() == null) {
            return first;
        }
        return first + ", " + code.getStartTime2().format(HHMM) + "-" + code.getEndTime2().format(HHMM);
    }

    /**
     * The hotel-local moment a shift on {@code date} begins: its first interval's start, or the start of the
     * day for a code with no times (an open schedule claims nothing about when).
     */
    public static LocalDateTime start(LocalDate date, ShiftCodeEntity code) {
        LocalTime start = code != null && code.getStartTime1() != null ? code.getStartTime1() : LocalTime.MIN;
        return date.atTime(start);
    }
}
