package com.sunsetbeach.mapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * The DB column is timestamp(3) (millisecond precision), but @UtcCreationTimestamp/@UtcUpdateTimestamp
 * populate the Java object with the JVM clock's full precision before it's ever round-tripped
 * through Postgres - truncate here so freshly-written rows serialize with exactly 3 fractional
 * digits, matching Prisma's DateTime JSON output, instead of leaking microsecond/nanosecond noise.
 */
public final class TimestampFormat {

    private static final DateTimeFormatter READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimestampFormat() {
    }

    /**
     * The current time as a UTC wall-clock {@code LocalDateTime} - the form every timestamp column
     * here stores and {@link #toUtc} expects. Use this, never {@code LocalDateTime.now()} (JVM
     * default zone) or {@code LocalDateTime.now(clock)} (the hotel's zone), for a value that is
     * written to such a column or compared against one.
     */
    public static LocalDateTime nowUtc(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    /**
     * A stored (UTC wall-clock) timestamp as hotel wall-clock, for comparing it with something the
     * hotel schedules in its own time - a spa appointment's date and start time, a stay date.
     * Comparing the stored value directly is off by the hotel's offset (seven hours).
     */
    public static LocalDateTime inHotelZone(LocalDateTime utcValue, Clock clock) {
        return utcValue.atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDateTime();
    }

    public static OffsetDateTime toUtc(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MILLIS).atOffset(ZoneOffset.UTC);
    }

    /**
     * Human-readable form for CSV exports (Excel columns, not JSON): whole seconds, no
     * milliseconds, explicit "UTC" suffix since a bare "yyyy-MM-dd HH:mm:ss" would otherwise
     * read as local time to whoever opens the file.
     */
    public static String readable(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS).format(READABLE) + " UTC";
    }

    /**
     * What goes on a printed ticket, receipt or Z report: the hotel's own wall-clock, no zone
     * suffix - the people reading paper at the kitchen pass or the till are on hotel time, and a
     * "17:30 UTC" at 00:30 local reads as the wrong hour. {@code utcValue} is a UTC timestamp
     * column or {@link #nowUtc}; files and exports keep {@link #readable}'s explicit UTC.
     */
    public static String readableHotel(LocalDateTime utcValue, Clock clock) {
        return inHotelZone(utcValue, clock).truncatedTo(ChronoUnit.SECONDS).format(READABLE);
    }
}
