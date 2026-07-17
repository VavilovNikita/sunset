package com.sunsetbeach.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * The DB column is timestamp(3) (millisecond precision), but @CreationTimestamp/@UpdateTimestamp
 * populate the Java object with the JVM clock's full precision before it's ever round-tripped
 * through Postgres - truncate here so freshly-written rows serialize with exactly 3 fractional
 * digits, matching Prisma's DateTime JSON output, instead of leaking microsecond/nanosecond noise.
 */
public final class TimestampFormat {

    private TimestampFormat() {
    }

    public static OffsetDateTime toUtc(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MILLIS).atOffset(ZoneOffset.UTC);
    }
}
