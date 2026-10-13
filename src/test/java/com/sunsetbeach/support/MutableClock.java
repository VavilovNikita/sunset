package com.sunsetbeach.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A test clock that stands still until a test moves it - the project's rule is that tests never read
 * the wall clock, and the gym automation is all about "what happens when time reaches X".
 * Hotel zone, like the production {@code Clock} bean.
 */
public class MutableClock extends Clock {

    public static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    private volatile Instant instant;

    public MutableClock(String hotelLocalDateTime) {
        set(hotelLocalDateTime);
    }

    /** Moves the clock to a hotel wall-clock time, e.g. {@code "2031-06-15T09:30"}. */
    public void set(String hotelLocalDateTime) {
        this.instant = LocalDateTime.parse(hotelLocalDateTime).atZone(HOTEL_ZONE).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return HOTEL_ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
