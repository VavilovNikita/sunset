package com.sunsetbeach.service;

import com.sunsetbeach.entity.AttendancePunchEntity;
import com.sunsetbeach.model.PunchDirection;
import java.time.Duration;
import java.util.List;

/**
 * Pairs consecutive IN/OUT punches within a single day into worked minutes - the one pairing rule
 * this system has (see {@code AttendancePunchEntity}'s own javadoc: pairing happens at read time,
 * never at write time). Shared by {@link AttendanceService#summary} (one employee, one month, per
 * day) and {@link RosterExportService}'s actuals export (every employee, one month, per day), so
 * there is exactly one copy of "how do two punches become a worked interval" rather than one per
 * caller.
 */
final class AttendancePunchPairing {

    private AttendancePunchPairing() {
    }

    /**
     * {@code dayPunches} must already be sorted by {@code punchAt} and belong to a single calendar
     * day - both callers get this for free from how they already fetch punches (see each caller's
     * own grouping). An unpaired trailing punch (an incomplete day) contributes nothing rather than
     * a guess.
     */
    static int sumWorkedMinutes(List<AttendancePunchEntity> dayPunches) {
        int minutes = 0;
        for (int i = 0; i + 1 < dayPunches.size(); i += 2) {
            AttendancePunchEntity in = dayPunches.get(i);
            AttendancePunchEntity out = dayPunches.get(i + 1);
            if (in.getDirection() == PunchDirection.IN && out.getDirection() == PunchDirection.OUT) {
                minutes += (int) Duration.between(in.getPunchAt(), out.getPunchAt()).toMinutes();
            }
        }
        return minutes;
    }
}
