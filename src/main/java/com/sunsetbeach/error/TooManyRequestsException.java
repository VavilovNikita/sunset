package com.sunsetbeach.error;

/**
 * 429 with the plain {@code { error: "..." }} shape - thrown by the login rate limiter
 * once an (ip, email) pair has racked up too many failed attempts.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
