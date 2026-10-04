package com.sunsetbeach.entity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

/**
 * Fills a {@code LocalDateTime} audit column with the current UTC wall-clock time, whatever the
 * JVM's default zone is. Every timestamp column in this schema is {@code timestamp without time
 * zone} holding UTC, and {@code TimestampFormat.toUtc} labels it as such on the way out.
 *
 * <p>This replaces Hibernate's own {@code @UtcCreationTimestamp}/{@code @UtcUpdateTimestamp}, which for
 * a {@code LocalDateTime} property read the JVM's <em>default-zone</em> clock (Hibernate 7's
 * {@code CurrentTimestampGeneration} falls back to {@code Clock.systemDefaultZone()} unless the
 * {@code hibernate.testing.clock} setting is present). That was only correct because production
 * happens to run in UTC: a backend started on a machine in Europe/Moscow wrote every new
 * {@code createdAt} three hours ahead, with nothing failing. {@code UtcTimestampDefaultZoneTests}
 * pins this down.
 */
public abstract class UtcTimestampGenerator implements BeforeExecutionGenerator {

    private static final Clock UTC = Clock.systemUTC();

    private final EnumSet<EventType> eventTypes;

    UtcTimestampGenerator(EnumSet<EventType> eventTypes) {
        this.eventTypes = eventTypes;
    }

    /** Microseconds - the most a Postgres timestamp column keeps. */
    static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC).truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return nowUtc();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return eventTypes;
    }

    public static final class OnInsert extends UtcTimestampGenerator {
        public OnInsert() {
            super(EnumSet.of(EventType.INSERT));
        }
    }

    public static final class OnInsertAndUpdate extends UtcTimestampGenerator {
        public OnInsertAndUpdate() {
            super(EnumSet.of(EventType.INSERT, EventType.UPDATE));
        }
    }
}
