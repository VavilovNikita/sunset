package com.sunsetbeach.lighting;

/**
 * A failure talking to a WLED controller, already worded for the person at the screen. Never carries
 * what the device sent back (a body, an exception message) - that is logged where it happened. The
 * service adds the controller's name in front; all kinds are a 502.
 */
public class WledException extends RuntimeException {

    public enum Kind {
        /** The address is not an allowed private LAN address (checked at request time). */
        REFUSED_ADDRESS,
        /** No answer: refused, timed out, unreachable. */
        UNREACHABLE,
        /** It answered, but with an error, a redirect or something that is not what WLED sends. */
        BAD_RESPONSE
    }

    private final Kind kind;

    public WledException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
