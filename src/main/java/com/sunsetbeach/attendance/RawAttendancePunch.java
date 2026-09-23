package com.sunsetbeach.attendance;

import com.sunsetbeach.model.PunchDirection;
import java.time.LocalDateTime;

/**
 * One record exactly as a terminal reported it - the device's own enrollment number (not yet
 * resolved to a {@code User}) and the device's own clock reading for when it happened, before
 * this system's own timestamp (when it was ingested) exists at all. See {@code
 * AttendanceService#ingestDevicePunch}'s own javadoc for what happens to this next.
 *
 * <p>{@code direction} is the device's own best-effort claim about which way this punch went -
 * never authoritative, and null when the device sent a punch code with no known direction mapping
 * (see {@code ZkTerminalClientImpl#directionOf}). A live K60 was found labeling several
 * consecutive same-day scans all "In", so {@code ingestDevicePunch} derives the real direction
 * server-side from this employee's own punch history and uses this field only as a diagnostic
 * hint to compare against, logging when the two disagree.
 */
public record RawAttendancePunch(int enrollmentNumber, LocalDateTime deviceTimestamp, PunchDirection direction) {
}
