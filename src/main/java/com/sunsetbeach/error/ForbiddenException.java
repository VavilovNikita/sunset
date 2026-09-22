package com.sunsetbeach.error;

/**
 * 403 with the plain {@code { error: "..." }} shape - distinct from {@link UnauthorizedException}
 * (401): the caller's identity/credentials are already established as legitimate, but the action
 * is refused for a business reason (e.g. {@code POST /guest-auth/login} with a correct password
 * against an unverified account - see that operation's own description for why this needs its
 * own status, not just a different 401 message).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
