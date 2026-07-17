package com.sunsetbeach.error;

/** 409 {@code { error: "..." }} - FK violations, duplicate emails, booking conflicts. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
