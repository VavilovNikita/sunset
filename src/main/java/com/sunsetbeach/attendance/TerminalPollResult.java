package com.sunsetbeach.attendance;

import java.util.List;

/**
 * One poll's outcome: the punches read, and the attendance-record layout size ({@code
 * ZkTerminalClientImpl#parseAttendanceRecords}, 16 or 40 bytes) used to parse them. {@code
 * recordSize} is returned even when {@code since} was non-null (a windowed read echoes back
 * whatever {@code knownRecordSize} it was given) so the caller can persist it unconditionally
 * after every poll - see {@code AttendanceDevicePollService#markSeen} - without needing to know
 * whether this particular poll freshly detected it or already knew it. Null only when nothing has
 * ever been read from this device (an empty log, or the very first poll of a brand new device).
 */
public record TerminalPollResult(List<RawAttendancePunch> punches, Integer recordSize) {
}
