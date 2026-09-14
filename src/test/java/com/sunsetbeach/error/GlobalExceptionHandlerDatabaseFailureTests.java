package com.sunsetbeach.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.ErrorMessage;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

/**
 * The backstop for a database failure nobody translated (see {@link GlobalExceptionHandler}'s own
 * javadoc): must never leak the real database message - which can carry table/column/constraint
 * names or query fragments - to whoever is standing at the desk, and must never use the same
 * status/shape {@link GlobalExceptionHandler#handleConflict} uses for a confirmed, known-safe-to-
 * retry conflict, since this handler doesn't actually know that about whatever reached it.
 */
class GlobalExceptionHandlerDatabaseFailureTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUnrecognizedDatabaseFailure_dataAccessException_returns500WithAGenericMessage() {
        SQLException sensitive = new SQLException(
                "ERROR: null value in column \"guestEmail\" of relation \"Booking\" violates not-null constraint DETAIL: Failing row contains (secret@example.com, ...).",
                "23502");
        CannotAcquireLockException e = new CannotAcquireLockException("wrapper", sensitive);

        ResponseEntity<ErrorMessage> response = handler.handleUnrecognizedDatabaseFailure(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError())
                .isEqualTo("Something went wrong saving that. Please try again, or tell an administrator if it keeps happening.")
                .doesNotContain("guestEmail")
                .doesNotContain("secret@example.com")
                .doesNotContain("Booking");
    }

    @Test
    void handleUnrecognizedDatabaseFailure_transactionSystemException_alsoReturns500() {
        TransactionSystemException e = new TransactionSystemException("commit failed");

        ResponseEntity<ErrorMessage> response = handler.handleUnrecognizedDatabaseFailure(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).doesNotContain("commit failed");
    }

    /**
     * This handler's status/message must never collide with {@link
     * GlobalExceptionHandler#handleConflict}'s - a known, translated conflict is a 409 with a
     * specific sentence; an unrecognized database failure is a 500 with a generic one. Confusing
     * the two would mean claiming a specific, safe-to-retry guarantee this handler hasn't earned.
     */
    @Test
    void handleUnrecognizedDatabaseFailure_neverLooksLikeAConflictException() {
        ResponseEntity<ErrorMessage> conflictResponse = handler.handleConflict(new ConflictException("This table already has an appointment overlapping this time."));
        ResponseEntity<ErrorMessage> unrecognizedResponse =
                handler.handleUnrecognizedDatabaseFailure(new CannotAcquireLockException("wrapper", new SQLException("boom", "XX000")));

        assertThat(unrecognizedResponse.getStatusCode()).isNotEqualTo(conflictResponse.getStatusCode());
        assertThat(unrecognizedResponse.getBody().getError()).isNotEqualTo(conflictResponse.getBody().getError());
    }
}
