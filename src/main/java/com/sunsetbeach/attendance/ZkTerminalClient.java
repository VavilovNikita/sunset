package com.sunsetbeach.attendance;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import java.time.LocalDateTime;

/**
 * The seam between {@code AttendanceDevicePollService} and an actual terminal on the network -
 * everything above this interface (the poll loop, ingestion, {@code lastSeenAt} bookkeeping) is
 * built and tested against a fake implementation of this one method, so it's finished before any
 * hardware exists to test against for real. {@link ZkTerminalClientImpl} is the one real
 * implementation, for exactly one device model (ZKTeco K60) - see its own javadoc for why this
 * isn't a general multi-vendor abstraction.
 */
public interface ZkTerminalClient {

    /**
     * One full poll cycle: connect, read the attendance log, set the device's clock to now,
     * disconnect. Never clears the device's log - see {@link ZkTerminalClientImpl}'s own javadoc
     * for why. Throws {@link AttendanceDeviceException} on any failure (unreachable, timed out,
     * an unexpected response) rather than returning a partial or empty result - a caller can't
     * tell "genuinely nothing new" from "couldn't read it" otherwise, and those need different
     * handling (see {@code AttendanceDevicePollService}).
     *
     * @param since null reads the device's entire stored log - used for a device that has never
     *     been read before (no watermark to window from yet) and for a manual re-sync. Non-null
     *     attempts a ranged read from this point onward via the device's own CMD_ATTLOG_TIME_RRQ
     *     command, falling back automatically (within this same call, no exception thrown for it)
     *     to a full read if the device doesn't honor that command - see the implementation's own
     *     javadoc for why that fallback exists rather than being treated as an error.
     * @param knownRecordSize the record layout size previously detected on this device's last full
     *     read, required to parse a windowed response's records (a windowed read's own returned
     *     byte count can't be divided back into a record size the way a full read's can - see
     *     {@code AttendanceDeviceEntity#attendanceRecordSize}'s own javadoc). Ignored, and freshly
     *     redetected, when {@code since} is null. Only ever null together with {@code since}.
     */
    TerminalPollResult poll(AttendanceDeviceEntity device, LocalDateTime since, Integer knownRecordSize);
}
