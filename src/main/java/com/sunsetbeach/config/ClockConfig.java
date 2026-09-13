package com.sunsetbeach.config;

import java.time.Clock;
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
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
