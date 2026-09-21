package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.model.PunchDirection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
