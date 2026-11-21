package com.sunsetbeach.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateTextTests {

    @Test
    void datesReadLikeADate_notLikeAnIsoString() {
        assertThat(DateText.date(LocalDate.parse("2026-11-19"))).isEqualTo("19 Nov 2026");
        assertThat(DateText.date(LocalDate.parse("2026-01-05"))).isEqualTo("5 Jan 2026");
    }
}
