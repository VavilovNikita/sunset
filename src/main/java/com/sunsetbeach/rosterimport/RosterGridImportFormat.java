package com.sunsetbeach.rosterimport;

/**
 * The exact layout {@code RosterExportService#exportGrid} writes and {@link RosterGridImportParser}
 * reads back - shared so the writer and reader can never drift apart silently. Two kinds of
 * machine-readable metadata, chosen for what each needs to survive:
 *
 * <ul>
 *   <li>Per-row/per-cell facts (which {@code User} a row is, which {@code ShiftCode} a cell means)
 *       live as <b>hidden companion columns on the visible sheet itself</b>, immediately after the
 *       day columns - never on a separate sheet indexed by raw row number. A hand edit that inserts
 *       or deletes an employee row moves that row's cells (visible and hidden alike) together, so
 *       this association survives it; a separate metadata sheet keyed by row number would not - every
 *       row below the edit would silently point at the wrong hidden entry. This does NOT defend
 *       against a day *column* being inserted or reordered within the grid - same scope boundary
 *       {@code ScheduleWorkbookParser} already accepts for its own file.
 *   <li>Whole-file facts (the export timestamp, and the full historical definition of every
 *       distinct {@code ShiftCode} used that month, for recreating a retired one) live on a
 *       separate hidden sheet, since nothing about them is tied to a specific row.
 * </ul>
 */
public final class RosterGridImportFormat {

    private RosterGridImportFormat() {
    }

    /** VERY_HIDDEN sheet carrying the export timestamp and the shift-code dictionary - see this class's own javadoc. */
    public static final String METADATA_SHEET_NAME = "Roster Import Data";

    public static final String EXPORTED_AT_LABEL = "ExportedAt";
    public static final String SHIFT_CODES_BLOCK_LABEL = "ShiftCodes";

    /** One blank column between the visible grid and the hidden companion columns - defensive, not load-bearing. */
    public static final int HIDDEN_BLOCK_GAP_COLUMNS = 1;

    public static String visibleSheetName(int year, int month) {
        return "Roster " + year + "-" + String.format("%02d", month);
    }

    /** The hidden column carrying each row's own {@code User.id} - blank on a header/group/totals row. */
    public static int employeeIdColumn(int days) {
        return days + 1 + HIDDEN_BLOCK_GAP_COLUMNS;
    }

    /** The hidden column carrying the {@code ShiftCode.id} that produced the visible cell at day column {@code day} - blank if that cell was blank at export. */
    public static int shiftCodeIdColumn(int days, int day) {
        return employeeIdColumn(days) + day;
    }
}
