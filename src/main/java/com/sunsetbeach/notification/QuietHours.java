package com.sunsetbeach.notification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Quiet hours as a pure function of the hotel's wall clock. The window is {@code [start, end)}:
 * {@code start} itself is quiet, {@code end} itself is not, so a message raised at exactly 07:00 goes
 * out at once and one raised at 22:00:00 waits. A window with {@code start > end} crosses midnight
 * (22:00-07:00). {@code start == end} is no window - the settings refuse it, and it is treated as
 * "never quiet" here rather than "always quiet" so a bad value can't swallow every message.
 */
public final class QuietHours {

    private QuietHours() {
    }

    /** When quiet hours end, if {@code hotelNow} is inside them; empty when it is not. Hotel wall-clock either way. */
    public static Optional<LocalDateTime> endOfQuietHours(LocalDateTime hotelNow, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return Optional.empty();
        }
        LocalTime time = hotelNow.toLocalTime();
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end) ? Optional.of(hotelNow.toLocalDate().atTime(end)) : Optional.empty();
        }
        if (!time.isBefore(start)) {
            return Optional.of(hotelNow.toLocalDate().plusDays(1).atTime(end));
        }
        if (time.isBefore(end)) {
            return Optional.of(hotelNow.toLocalDate().atTime(end));
        }
        return Optional.empty();
    }
}
