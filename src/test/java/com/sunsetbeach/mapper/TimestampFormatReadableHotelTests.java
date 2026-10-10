package com.sunsetbeach.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Printed tickets show the hotel's wall-clock; exports keep the explicit UTC label. */
class TimestampFormatReadableHotelTests {

    private static final Clock BANGKOK = Clock.system(ZoneId.of("Asia/Bangkok"));

    @Test
    void aUtcValueIsPrintedAsHotelTime_withNoZoneSuffix() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 10, 17, 30, 45);

        assertThat(TimestampFormat.readableHotel(utc, BANGKOK)).isEqualTo("2026-10-11 00:30:45");
        assertThat(TimestampFormat.readable(utc)).isEqualTo("2026-10-10 17:30:45 UTC");
    }
}
