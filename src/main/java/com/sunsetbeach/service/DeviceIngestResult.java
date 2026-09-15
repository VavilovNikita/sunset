package com.sunsetbeach.service;

/** Outcome of one {@link AttendanceService#ingestDevicePunch} call - internal to the poll path, never serialized. */
public enum DeviceIngestResult {
    INGESTED,
    DUPLICATE,
    UNKNOWN_ENROLLMENT_NUMBER
}
