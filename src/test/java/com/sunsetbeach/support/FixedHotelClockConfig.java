package com.sunsetbeach.support;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The hotel clock pinned to 2034-03-10, 12:20 in Bangkok, for the test classes that need a known
 * "today" and "now". It is one shared class on purpose: every distinct combination of test beans is
 * its own cached Spring context with its own connection pool against the suite's single Postgres
 * (see {@code AbstractIntegrationTest}), and the pool total is what ran out when each such class
 * declared a clock of its own. Classes that {@code @Import} this one share a single context.
 */
@TestConfiguration
public class FixedHotelClockConfig {

    public static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
    public static final LocalDateTime NOW = LocalDateTime.parse("2034-03-10T12:20");

    @Bean
    @Primary
    Clock fixedHotelClock() {
        return Clock.fixed(NOW.atZone(BANGKOK).toInstant(), BANGKOK);
    }
}
