package com.sunsetbeach.rosterimport;

/**
 * One employee row on the visible grid sheet. {@code metadataEmployeeUserId} is set only when the
 * row still carries the hidden {@code User.id} column {@code RosterExportService} wrote for it -
 * null means this row has no export metadata at all (added, or its identity otherwise lost, by a
 * hand edit since export), so {@code rawName} is all there is to resolve it by.
 */
public record ParsedGridImportRow(int rowIndex, String rawName, String metadataEmployeeUserId) {
}
