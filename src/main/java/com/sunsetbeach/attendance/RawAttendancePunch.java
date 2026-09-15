package com.sunsetbeach.attendance;

import com.sunsetbeach.model.PunchDirection;
import java.time.LocalDateTime;

/**
 * One record exactly as a terminal reported it - the device's own enrollment number (not yet
 * resolved to a {@code User}) and the device's own clock reading for when it happened, before
 * this system's own timestamp (when it was ingested) exists at all. See {@code
 * AttendanceService#ingestDevicePunch}'s own javadoc for what happens to this next.
 */
public record RawAttendancePunch(int enrollmentNumber, LocalDateTime deviceTimestamp, PunchDirection direction) {
}
