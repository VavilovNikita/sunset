package com.sunsetbeach.rosterimport;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Everything {@link RosterGridImportParser#parse} could get out of one previously-exported grid file. */
public record ParsedGridImport(
        Instant exportedAt, Map<String, ShiftCodeSnapshot> shiftCodeSnapshots, List<ParsedGridImportRow> rows, List<ParsedGridImportCell> cells) {
}
