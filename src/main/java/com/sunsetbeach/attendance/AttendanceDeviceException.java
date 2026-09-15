package com.sunsetbeach.attendance;

/**
 * Any failure talking to a fingerprint terminal - unreachable, timed out, or an unexpected
 * response shape. A failed poll is normal (the thing is on a wall in a hotel) - see {@code
 * AttendanceDevicePollService}'s own javadoc for how a caller is expected to treat this: keep
 * polling, don't raise anything a person has to dismiss, but make a long run of failures visible
 * somewhere.
 */
public class AttendanceDeviceException extends RuntimeException {

    public AttendanceDeviceException(String message) {
        super(message);
    }

    public AttendanceDeviceException(String message, Throwable cause) {
        super(message, cause);
    }
}
