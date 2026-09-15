package com.sunsetbeach.rosterimport;

import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
import java.time.LocalDate;

/**
 * One successfully-read cell from the grid: a person, a date, and the code they were scheduled
 * for that day. {@code fillColor} is non-null only when {@code rawCode} is the literal text
 * {@code "9"} - see {@code ScheduleWorkbookParser}'s own javadoc for why that's the one code this
 * file can't tell apart by text alone.
 */
public record ParsedCell(String rawName, StaffArea staffArea, LocalDate date, String rawCode, FillColor fillColor, String cellRef) {
}
