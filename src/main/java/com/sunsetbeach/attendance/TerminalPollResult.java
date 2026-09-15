package com.sunsetbeach.attendance;

import java.util.List;

/**
 * One poll's outcome: the punches read, the attendance-record layout size ({@code
 * ZkTerminalClientImpl#parseAttendanceRecords}, 16 or 40 bytes) used to parse them, and whether
 * this specific poll found out anything new about whether the device honors a windowed read.
 *
 * <p>{@code recordSize} is returned even when {@code since} was non-null (a windowed read echoes
 * back whatever {@code knownRecordSize} it was given) so the caller can persist it unconditionally
 * after every poll - see {@code AttendanceDevicePollService#markSeen} - without needing to know
 * whether this particular poll freshly detected it or already knew it. Null only when nothing has
 * ever been read from this device (an empty log, or the very first poll of a brand new device).
 *
 * <p>{@code windowedReadUnsupported} is {@code null} unless this poll actually attempted a
 * windowed read ({@code since} was non-null and there was something to read) - a full read taken
 * because there was no watermark yet, or because the log was empty, says nothing about whether
 * the device supports the windowed command, so it must leave this field alone rather than assert
 * either way. {@code true} means the device rejected {@code CMD_ATTLOG_TIME_RRQ} this poll and the
 * call fell back to a full read within the same call (see {@code
 * ZkTerminalClientImpl#readWindowedAttendanceLog}); {@code false} means a windowed read was
 * attempted and the device actually honored it. Either way the caller persists this as the new
 * truth (see {@code AttendanceDeviceEntity#windowedReadUnsupported}) rather than only ever setting
 * it and never clearing it - firmware can be updated, or a replacement unit swapped in under the
 * same row, after which the device may start (or stop) answering the ranged command.
 */
public record TerminalPollResult(List<RawAttendancePunch> punches, Integer recordSize, Boolean windowedReadUnsupported) {
}
