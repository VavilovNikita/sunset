package com.sunsetbeach.rosterimport;

import java.time.LocalDate;

/**
 * One non-blank cell on the visible grid sheet - a blank cell (day off) is never represented here,
 * same "absence, not a row" convention {@code RosterEntry} itself uses. {@code metadataShiftCodeId}
 * is the id the hidden companion column for this exact cell still carries, or null if this cell has
 * no export metadata at all (blank at export - an {@code ADD} - or its hidden column was otherwise
 * lost). {@link com.sunsetbeach.service.RosterGridImportService} still cross-checks
 * {@code metadataShiftCodeId} against {@code rawCode} before trusting it - a hand-retyped
 * {@code rawCode} does not update this hidden column, so a mismatch here means "trust {@code
 * rawCode} instead, this cell was edited by hand since export", never "trust the stale id".
 */
public record ParsedGridImportCell(int rowIndex, int day, LocalDate date, String rawCode, String metadataShiftCodeId) {
}
