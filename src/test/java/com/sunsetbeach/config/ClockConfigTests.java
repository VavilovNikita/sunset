package com.sunsetbeach.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * The hotel is in Thailand (ICT, UTC+7); the server has no {@code TZ} set at the container/OS
 * level, so a clock built off the ambient default zone runs on UTC in production. Between
 * roughly 00:00 and 06:59 Bangkok time, that resolves "today" to the previous calendar day for
 * anything reading it off this clock - this locks {@link ClockConfig#clock()}'s zone to
 * {@code Asia/Bangkok} explicitly so that regression can't silently return.
 */
class ClockConfigTests {

    @Test
    void clockIsPinnedToBangkokRegardlessOfAmbientDefaultZone() {
        Clock clock = new ClockConfig().clock();
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Bangkok"));
    }

    @Test
    void earlyMorningBangkokTimeResolvesToBangkokDate_notTheUtcShiftedPreviousDay() {
        LocalDate bangkokDate = LocalDate.of(2026, 9, 23);
        Instant instant = bangkokDate.atTime(2, 0).atZone(ZoneId.of("Asia/Bangkok")).toInstant();

        Clock fixedAtBangkokZone = Clock.fixed(instant, ZoneId.of("Asia/Bangkok"));
        assertThat(LocalDate.now(fixedAtBangkokZone)).isEqualTo(bangkokDate);

        // Same instant, read through a UTC-zoned clock (what Clock.systemDefaultZone() would have
        // produced in production, absent a TZ env var) - this is the previous-day bug.
        Clock fixedAtUtc = Clock.fixed(instant, ZoneId.of("UTC"));
        assertThat(LocalDate.now(fixedAtUtc)).isEqualTo(bangkokDate.minusDays(1));
    }
}
