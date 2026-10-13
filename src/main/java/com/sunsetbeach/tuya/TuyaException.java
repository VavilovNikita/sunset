package com.sunsetbeach.tuya;

/**
 * A failure talking to Tuya, already worded for the person at the screen ({@link #getMessage()}).
 * The raw Tuya code never goes into the message - it is logged where it happened.
 */
public class TuyaException extends RuntimeException {

    public enum Kind {
        /** TUYA_* keys are not set on this server. */
        NOT_CONFIGURED,
        /** The project cannot be used at all (keys refused, subscription expired, API not subscribed). */
        UNAVAILABLE,
        /** Tuya answered but refused this request. */
        REJECTED,
        /** No usable answer - timeout, connection error, 5xx. */
        NETWORK
    }

    private final Kind kind;

    public TuyaException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    /** The integration as a whole is down (as opposed to one request being refused). */
    public boolean isIntegrationDown() {
        return kind == Kind.NOT_CONFIGURED || kind == Kind.UNAVAILABLE;
    }
}
