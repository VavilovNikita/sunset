package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.model.PunchDirection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pairs consecutive IN/OUT punches within a single day into worked minutes - the one pairing rule
 * this system has (see {@code AttendancePunchEntity}'s own javadoc: pairing happens at read time,
 * never at write time). Shared by {@link AttendanceService#summary} (one employee, one month, per
 * day), {@link RosterExportService}'s actuals export (every employee, one month, per day), and
 * that same export's "Late & anomalies" sheet (per-interval late/left-early comparison), so there
 * is exactly one copy of "how do two punches become a worked interval" rather than one per caller.
 */
final class AttendancePunchPairing {

    private AttendancePunchPairing() {
    }

    /** One positional IN/OUT pair - see {@link #pairs} for how these are formed. */
    record PunchPair(AttendancePunchEntity in, AttendancePunchEntity out) {
    }

    /**
     * {@code dayPunches} must already be sorted by {@code punchAt} and belong to a single calendar
     * day - both callers get this for free from how they already fetch punches (see each caller's
     * own grouping). Pairs are formed positionally (1st+2nd, 3rd+4th, ...), not by matching
     * direction - a trailing unpaired punch (an incomplete day) is simply left out of the result
     * rather than guessed at.
     */
    static List<PunchPair> pairs(List<AttendancePunchEntity> dayPunches) {
        List<PunchPair> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < dayPunches.size(); i += 2) {
            pairs.add(new PunchPair(dayPunches.get(i), dayPunches.get(i + 1)));
        }
        return pairs;
    }

    /**
     * When someone was at work during one planned interval. A span of presence is a complete pair
     * (IN to OUT) or a trailing open IN (IN until {@code now}); it covers every interval it overlaps,
     * so one pair 11:00-21:00 covers both halves of a 11:00-14:00 + 16:00-21:00 split shift. {@code
     * arrivedAt} is the start of the first overlapping span (before the interval's start means
     * "was already there": no lateness), {@code leftAt} the end of the last one, null while still in.
     *
     * <p>This is the one definition of "was this interval attended" - the Today board, the
     * notification scan and the actuals export's "Late &amp; anomalies" sheet all ask it, so they
     * cannot disagree. It reads the same positional pairs as {@link #pairs}; it changes neither the
     * IN/OUT parity rule nor the worked-minutes sum.
     */
    record IntervalPresence(LocalDateTime arrivedAt, LocalDateTime leftAt) {
    }

    static Optional<IntervalPresence> presenceIn(List<AttendancePunchEntity> dayPunches, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        LocalDateTime arrivedAt = null;
        LocalDateTime leftAt = null;
        for (PunchPair pair : pairs(dayPunches)) {
            LocalDateTime in = pair.in().getPunchAt();
            LocalDateTime out = pair.out().getPunchAt();
            if (in.isBefore(end) && out.isAfter(start)) {
                if (arrivedAt == null) arrivedAt = in;
                leftAt = out;
            }
        }
        if (hasTrailingUnmatchedIn(dayPunches)) {
            LocalDateTime in = dayPunches.get(dayPunches.size() - 1).getPunchAt();
            if (in.isBefore(end) && !now.isBefore(start)) {
                if (arrivedAt == null) arrivedAt = in;
                leftAt = null;
            }
        }
        return arrivedAt == null ? Optional.empty() : Optional.of(new IntervalPresence(arrivedAt, leftAt));
    }

    static boolean hasTrailingUnmatchedIn(List<AttendancePunchEntity> dayPunches) {
        return dayPunches.size() % 2 == 1 && dayPunches.get(dayPunches.size() - 1).getDirection() == PunchDirection.IN;
    }

    static int sumWorkedMinutes(List<AttendancePunchEntity> dayPunches) {
        int minutes = 0;
        for (PunchPair pair : pairs(dayPunches)) {
            if (pair.in().getDirection() == PunchDirection.IN && pair.out().getDirection() == PunchDirection.OUT) {
                minutes += (int) Duration.between(pair.in().getPunchAt(), pair.out().getPunchAt()).toMinutes();
            }
        }
        return minutes;
    }
}
