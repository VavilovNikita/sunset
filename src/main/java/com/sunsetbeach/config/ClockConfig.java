package com.sunsetbeach.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one seam between "what time is it right now" and code that needs to know. Everywhere else
 * in this codebase either takes a date as an explicit caller-supplied parameter or doesn't need
 * "now" at all - {@link com.sunsetbeach.service.SpaMapService} is the exception, since the spa
 * floor plan's whole purpose is showing what's true about this exact moment. Injecting a
 * {@link Clock} rather than calling {@code LocalDate.now()}/{@code LocalTime.now()} directly lets
 * a test substitute a fixed one ({@code Clock.fixed(...)}), so "is this appointment upcoming"
 * doesn't depend on what wall-clock time the suite happens to run at - see
 * {@code SpaMapServiceTests}' own nested {@code @TestConfiguration}.
 *
 * <p>Pinned to the hotel's own zone explicitly, not {@code Clock.systemDefaultZone()} - the
 * hotel is in Thailand (ICT, UTC+7), and the server has no {@code TZ} set at the container/OS
 * level, so the ambient default zone is UTC in production. Between roughly 00:00 and 06:59
 * Bangkok time, that made "today" resolve to the previous calendar day for anything reading it
 * off this clock. Same "explicit over ambient" reasoning as injecting {@link Clock} in the first
 * place - don't make this depend on {@code TZ} being set correctly at the OS level either.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Bangkok"));
    }
}
