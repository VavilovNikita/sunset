package com.sunsetbeach.rosterimport;

import java.util.List;

/** Everything {@link ScheduleWorkbookParser#parse} could get out of one sheet - the good cells and the bad ones, both collected, never just the first of either. */
public record ParsedSchedule(List<ParsedCell> cells, List<ParseIssue> issues) {
}
