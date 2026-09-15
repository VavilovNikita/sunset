package com.sunsetbeach.attendance;

import com.sunsetbeach.entity.AttendanceDeviceEntity;
import java.util.List;

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
     */
    List<RawAttendancePunch> poll(AttendanceDeviceEntity device);
}
