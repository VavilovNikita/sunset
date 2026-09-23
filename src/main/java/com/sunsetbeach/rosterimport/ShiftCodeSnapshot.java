package com.sunsetbeach.rosterimport;

import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;

/**
 * The full definition of one {@code ShiftCode} row, exactly as it stood at export time - one of
 * these is written to the hidden {@code ShiftCodes} block for every distinct id a grid export
 * actually used that month. {@code ShiftCode} rows are never mutated once referenced (see that
 * entity's own javadoc), so this snapshot stays a faithful historical record even after {@code id}
 * itself stops resolving to an *active* row - which is exactly the situation
 * {@code RosterGridImportService} uses this for: recreating a retired code with its own original
 * shape rather than asking the admin to redefine it from nothing.
 */
public record ShiftCodeSnapshot(
        String id,
        String code,
        StaffArea staffArea,
        ShiftCodeKind kind,
        String startTime1,
        String endTime1,
        String startTime2,
        String endTime2,
        boolean countsAsWorked,
        boolean isPaid,
        String effectiveFrom,
        String displayColor) {
}
