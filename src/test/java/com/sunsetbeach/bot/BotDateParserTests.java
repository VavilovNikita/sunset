package com.sunsetbeach.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The typed date of the shift-move dialogue: formats, the year-less rule, the Buddhist year, and what is no date. */
class BotDateParserTests {

    private static final LocalDate TODAY = LocalDate.of(2033, 5, 17);

    private static LocalDate parse(String text, LocalDate today) {
        return BotDateParser.parse(text, today).orElse(null);
    }

    @Test
    void everyFormat_givesTheSameDay() {
        for (String typed : new String[] {"25.06.2033", "25/06/2033", "2033-06-25", " 25.06.2033 ", "25.6.2033", "2033-6-25"}) {
            assertThat(parse(typed, TODAY)).as(typed).isEqualTo(LocalDate.of(2033, 6, 25));
        }
    }

    @Test
    void withoutAYear_itIsTheNearestDateNotEarlierThanToday() {
        assertThat(parse("25.06", TODAY)).isEqualTo(LocalDate.of(2033, 6, 25));
        assertThat(parse("25/06", TODAY)).isEqualTo(LocalDate.of(2033, 6, 25));
        assertThat(parse("17.05", TODAY)).as("today itself").isEqualTo(TODAY);
        assertThat(parse("16.05", TODAY)).as("yesterday is next year").isEqualTo(LocalDate.of(2034, 5, 16));
        assertThat(parse("01.01", TODAY)).isEqualTo(LocalDate.of(2034, 1, 1));
    }

    @Test
    void theHotelDayDecidesAtMidnight() {
        // The same text, one hour of the clock apart across the hotel's midnight, is two different dates.
        assertThat(parse("19.10", LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(parse("19.10", LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2027, 10, 19));
    }

    @Test
    void aYearAbove2500_isBuddhist() {
        assertThat(parse("25.06.2576", TODAY)).isEqualTo(LocalDate.of(2033, 6, 25));
        assertThat(parse("2576-06-25", TODAY)).isEqualTo(LocalDate.of(2033, 6, 25));
        assertThat(parse("19.10.2569", LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(parse("25.06.2500", TODAY)).as("2500 is still a Gregorian year").isEqualTo(LocalDate.of(2500, 6, 25));
    }

    @Test
    void aLeapDayWithoutAYear_waitsForTheNextLeapYear() {
        assertThat(parse("29.02", LocalDate.of(2033, 1, 1))).isEqualTo(LocalDate.of(2036, 2, 29));
        assertThat(parse("29.02", LocalDate.of(2096, 3, 1))).as("2100 is not a leap year").isEqualTo(LocalDate.of(2104, 2, 29));
    }

    @Test
    void whatIsNoDate_isEmpty_notAGuess() {
        for (String typed : new String[] {"", "   ", "hello", "31.02", "31.04", "32.01", "00.05", "10.13", "29.02.2033", "5", "2033", "25-06-2033", "25.06.33", "25.06.2033 14:00", "tomorrow"}) {
            assertThat(BotDateParser.parse(typed, TODAY)).as(typed).isEmpty();
        }
        assertThat(BotDateParser.parse(null, TODAY)).isEmpty();
    }
}
