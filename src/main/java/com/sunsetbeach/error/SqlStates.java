package com.sunsetbeach.error;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Recognises a database failure by its SQLSTATE, walking the exception's cause chain to find the
 * underlying {@link SQLException} - never by matching prose in an error message. Message wording
 * is not an API: it varies by driver/server version and locale, and some failures don't even name
 * the thing they were checking when they landed - a Postgres deadlock's own message names the
 * relation it was checking an exclusion constraint on, never the constraint itself, which is
 * exactly why {@code SpaAppointmentService#translateOverlap} used to miss it entirely while
 * string-matching for a constraint name that was never going to be there. See CLAUDE.md's
 * Concurrency section.
 */
public final class SqlStates {

    private SqlStates() {
    }

    /** The first {@link SQLException} in the cause chain whose {@code getSQLState()} matches, if any. */
    public static Optional<SQLException> find(Throwable e, String sqlState) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return Optional.of(sqlException);
            }
        }
        return Optional.empty();
    }

    public static boolean is(Throwable e, String sqlState) {
        return find(e, sqlState).isPresent();
    }
}
