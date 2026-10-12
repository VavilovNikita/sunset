package com.sunsetbeach.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A "day" in a date filter is a hotel (Asia/Bangkok) day, but every timestamp column holds UTC
 * wall-clock - so the boundary of a day is the hotel's local midnight expressed in UTC. Cutting at
 * {@code day.atStartOfDay()} (UTC midnight) put a payment taken at 02:00 Bangkok on the previous
 * day. One conversion for every date filter over a {@code createdAt}/{@code openedAt}-style column:
 * compare with {@code >= startUtc(from)} and {@code < startUtc(to.plusDays(1))}. See
 * {@link ReportDateRange}, which does the same for the report endpoints.
 */
final class HotelDay {

    private HotelDay() {
    }

    static LocalDateTime startUtc(LocalDate day, ZoneId hotelZone) {
        return day.atStartOfDay(hotelZone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
