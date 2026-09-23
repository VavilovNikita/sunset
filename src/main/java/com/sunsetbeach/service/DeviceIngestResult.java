package com.sunsetbeach.service;

/** Outcome of one {@link AttendanceService#ingestDevicePunch} call - internal to the poll path, never serialized. */
public enum DeviceIngestResult {
    INGESTED,
    DUPLICATE,
    UNKNOWN_ENROLLMENT_NUMBER,
    /**
     * A misread fingerprint or a habitual double-tap, close enough behind this employee's most
     * recent punch (any source, same calendar day) to be an accidental repeat rather than a real
     * direction change - see {@code AttendanceService#ingestDevicePunch}'s own javadoc. Not
     * written as a row at all, so it can't flip that day's IN/OUT parity.
     */
    IGNORED_DUPLICATE_SCAN
}
