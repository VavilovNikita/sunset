package com.sunsetbeach.error;

/**
 * 400 with the plain {@code { error: "..." }} shape (not the Zod-flatten ValidationError
 * shape) - matches ad-hoc hand-written checks in the route handlers, e.g. "Range too large
 * (max 366 days)", "No files provided", "You can't change your own role".
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
