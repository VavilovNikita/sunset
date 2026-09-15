package com.sunsetbeach.rosterimport;

/** One cell (or, with {@code cellRef} null, the sheet as a whole) that could not be read, and why. */
public record ParseIssue(String cellRef, String message) {
}
