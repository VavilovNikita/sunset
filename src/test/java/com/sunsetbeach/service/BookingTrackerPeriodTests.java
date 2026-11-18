package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** The tracker's month range, default season and the Friday the snapshot job owes - all pure date logic. */
class BookingTrackerPeriodTests {

    @Test
    void defaultSeason_isTheOneContainingToday_orTheComingOneFromMayToOctober() {
        assertSeason(LocalDate.of(2026, 11, 18), "2026-11", "2027-04");
        assertSeason(LocalDate.of(2026, 11, 1), "2026-11", "2027-04");
        assertSeason(LocalDate.of(2026, 12, 31), "2026-11", "2027-04");
        assertSeason(LocalDate.of(2027, 1, 1), "2026-11", "2027-04");
        assertSeason(LocalDate.of(2027, 4, 30), "2026-11", "2027-04");
        assertSeason(LocalDate.of(2027, 5, 1), "2027-11", "2028-04");
        assertSeason(LocalDate.of(2027, 10, 31), "2027-11", "2028-04");
    }

    private static void assertSeason(LocalDate today, String from, String to) {
        BookingTrackerPeriod p = BookingTrackerPeriod.parse(null, null, null, today);
        assertThat(p.from()).as(today.toString()).isEqualTo(YearMonth.parse(from));
        assertThat(p.to()).as(today.toString()).isEqualTo(YearMonth.parse(to));
        assertThat(p.months()).hasSize(6);
    }

    @Test
    void nextSeason_isTwelveMonthsOn() {
        BookingTrackerPeriod next = BookingTrackerPeriod.seasonFor(LocalDate.of(2027, 2, 1)).nextSeason();
        assertThat(next.from()).isEqualTo(YearMonth.of(2027, 11));
        assertThat(next.to()).isEqualTo(YearMonth.of(2028, 4));
    }

    @Test
    void explicitRange_isInclusive_andStayMonthMustLieInside() {
        BookingTrackerPeriod p = BookingTrackerPeriod.parse("2026-11", "2027-02", "2027-01", LocalDate.of(2026, 11, 18));
        assertThat(p.months()).extracting(Object::toString).containsExactly("2026-11", "2026-12", "2027-01", "2027-02");
        assertThat(p.filterMonths()).containsExactly(YearMonth.of(2027, 1));
        assertThat(BookingTrackerPeriod.parse("2026-11", "2027-02", null, LocalDate.of(2026, 11, 18)).filterMonths()).hasSize(4);
        assertThat(p.firstDay()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(p.lastDay()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void invalidRanges_areValidationErrors() {
        LocalDate today = LocalDate.of(2026, 11, 18);
        assertThatThrownBy(() -> BookingTrackerPeriod.parse("2026-11", null, null, today)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> BookingTrackerPeriod.parse(null, "2026-11", null, today)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> BookingTrackerPeriod.parse("2027-02", "2026-11", null, today)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> BookingTrackerPeriod.parse("2026-13", "2027-01", null, today)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> BookingTrackerPeriod.parse("2026-01", "2027-01", null, today)).isInstanceOf(ValidationException.class); // 13 months
        assertThatThrownBy(() -> BookingTrackerPeriod.parse("2026-11", "2027-02", "2027-03", today)).isInstanceOf(ValidationException.class);
        assertThat(BookingTrackerPeriod.parse("2026-01", "2026-12", null, today).months()).hasSize(12); // exactly 12 is fine
    }

    @Test
    void latestDueFriday_isTheLatestFridayWhose2355HasPassed() {
        // 2034-03-10 is a Friday.
        assertThat(BookingTrackerSnapshotService.latestDueFriday(LocalDateTime.parse("2034-03-10T23:54:59"))).isEqualTo(LocalDate.of(2034, 3, 3));
        assertThat(BookingTrackerSnapshotService.latestDueFriday(LocalDateTime.parse("2034-03-10T23:55:00"))).isEqualTo(LocalDate.of(2034, 3, 10));
        assertThat(BookingTrackerSnapshotService.latestDueFriday(LocalDateTime.parse("2034-03-11T10:00"))).isEqualTo(LocalDate.of(2034, 3, 10));
        assertThat(BookingTrackerSnapshotService.latestDueFriday(LocalDateTime.parse("2034-03-14T10:00"))).isEqualTo(LocalDate.of(2034, 3, 10));
        assertThat(BookingTrackerSnapshotService.latestDueFriday(LocalDateTime.parse("2034-03-09T10:00"))).isEqualTo(LocalDate.of(2034, 3, 3));
    }
}
