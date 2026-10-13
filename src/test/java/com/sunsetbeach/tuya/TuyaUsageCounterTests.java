package com.sunsetbeach.tuya;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The monthly call counter: a month is a Bangkok month (a call at 00:30 on the 1st there is still the
 * 31st in UTC), counts add up, and months stay apart.
 */
@SpringBootTest
class TuyaUsageCounterTests extends AbstractIntegrationTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM \"TuyaApiUsage\" WHERE month IN ('2031-01', '2031-02')");
    }

    @Test
    void aCallJustAfterBangkokMidnight_belongsToTheNewBangkokMonth() {
        // 2031-01-31T17:00:00Z is 2031-02-01 00:00 in Bangkok; one second earlier is still January there.
        TuyaUsageCounter january = counterAt("2031-01-31T16:59:59Z");
        TuyaUsageCounter february = counterAt("2031-01-31T17:00:00Z");

        assertThat(january.currentMonth()).isEqualTo("2031-01");
        assertThat(february.currentMonth()).isEqualTo("2031-02");

        january.increment();
        january.increment();
        february.increment();

        assertThat(january.callsThisMonth()).isEqualTo(2);
        assertThat(february.callsThisMonth()).isEqualTo(1);
    }

    @Test
    void aMonthWithNoCalls_readsZero() {
        assertThat(counterAt("2031-01-10T03:00:00Z").callsThisMonth()).isZero();
    }

    private TuyaUsageCounter counterAt(String instant) {
        return new TuyaUsageCounter(jdbc, txManager, Clock.fixed(Instant.parse(instant), BANGKOK));
    }
}
