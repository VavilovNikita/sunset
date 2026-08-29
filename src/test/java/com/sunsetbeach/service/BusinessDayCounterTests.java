package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Fixed reference dates (never {@link java.time.LocalDate#now()}) so these are deterministic
 * regardless of which real weekday the suite happens to run on. 2024-01-01 is a Monday (a known,
 * checkable fact) - every date below is expressed relative to it rather than trusted blindly.
 */
class BusinessDayCounterTests {

    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 8);
    private static final LocalDate TUESDAY = LocalDate.of(2024, 1, 9);
    private static final LocalDate FRIDAY = LocalDate.of(2024, 1, 5);
    private static final LocalDate SATURDAY = LocalDate.of(2024, 1, 6);
    private static final LocalDate SUNDAY = LocalDate.of(2024, 1, 7);

    @Test
    void sameDay_isZero() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(FRIDAY, FRIDAY)).isZero();
    }

    @Test
    void fridayToSaturday_isZero() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(FRIDAY, SATURDAY)).isZero();
    }

    @Test
    void fridayToSunday_isZero() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(FRIDAY, SUNDAY)).isZero();
    }

    @Test
    void fridayToMonday_isOne_weekendDoesNotCount() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(FRIDAY, MONDAY)).isEqualTo(1);
    }

    @Test
    void fridayToTuesday_isTwo() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(FRIDAY, TUESDAY)).isEqualTo(2);
    }

    @Test
    void mondayToTuesday_isOne_noWeekendInBetween() {
        assertThat(BusinessDayCounter.countBusinessDaysBetween(MONDAY, TUESDAY)).isEqualTo(1);
    }
}
