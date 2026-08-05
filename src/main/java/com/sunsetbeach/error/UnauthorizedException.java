package com.sunsetbeach.error;

/**
 * 401 with the plain {@code { error: "..." }} shape - used for login failures (bad
 * credentials), as opposed to Spring Security's RestAuthEntryPoint, which handles the
 * missing/invalid-token case for already-protected routes.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
