package com.sunsetbeach.rosterimport;

/**
 * A structural problem the parser cannot work around at all - the wrong sheet, or a day-number
 * header that isn't where this file format always puts it. Unlike a {@link ParseIssue} (one bad
 * cell among otherwise-readable ones, collected and reported alongside everything that did work),
 * this stops parsing immediately: nothing else in the sheet can be trusted once the grid's own
 * shape isn't what was assumed.
 */
public class ScheduleParseException extends RuntimeException {

    public ScheduleParseException(String message) {
        super(message);
    }
}
