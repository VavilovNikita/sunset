package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.error.ConflictException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@code SpaAppointmentService#translateOverlap} recognises a failure by its SQLSTATE (see {@code
 * com.sunsetbeach.error.SqlStates} and CLAUDE.md's Concurrency section), not by matching prose in
 * the message - these assert the actual translated sentence for each SQLSTATE, not merely that
 * something is thrown, specifically so a future regression back to message-matching (or a broken
 * SQLSTATE comparison) fails loudly here instead of surfacing as a raw 500 the next time
 * {@link SpaAppointmentOverlapRaceTests} happens to hit a real deadlock.
 */
class SpaAppointmentOverlapTranslationTests {

    /**
     * The actual shape of a real Postgres deadlock hit while checking a deferred exclusion
     * constraint (see {@link SpaAppointmentOverlapRaceTests}) - it names the relation it was
     * checking a constraint on, never the constraint itself. This is exactly the message that
     * used to fall through {@code translateOverlap}'s old string-matching untranslated.
     */
    @Test
    void translateOverlap_deadlock_translatesEvenThoughTheMessageNamesNoConstraint() {
        SQLException deadlock = new SQLException(
                "ERROR: deadlock detected\n  Detail: Process 72 waits for ShareLock on transaction 2543; blocked by process 73.\n"
                        + "Process 73 waits for ShareLock on transaction 2542; blocked by process 72.\n  Hint: See server log for query details.\n"
                        + "  Context: while checking exclusion constraint on tuple (0,25) in relation \"SpaAppointment\"",
                "40P01");
        DataAccessException e = new CannotAcquireLockException("deadlock", deadlock);

        ConflictException result = SpaAppointmentService.translateOverlap(e);

        assertThat(result.getMessage()).isEqualTo("Someone else was changing one of these appointments at the same time — please try again.");
    }

    @Test
    void translateOverlap_tableExclusionViolation_namesTheTable() {
        SQLException violation =
                new SQLException("ERROR: conflicting key value violates exclusion constraint \"spa_appointment_no_table_overlap\"", "23P01");
        DataAccessException e = new DataIntegrityViolationException("violation", violation);

        ConflictException result = SpaAppointmentService.translateOverlap(e);

        assertThat(result.getMessage()).isEqualTo("This table already has an appointment overlapping this time.");
    }

    @Test
    void translateOverlap_therapistExclusionViolation_namesTheTherapist() {
        SQLException violation =
                new SQLException("ERROR: conflicting key value violates exclusion constraint \"spa_appointment_no_therapist_overlap\"", "23P01");
        DataAccessException e = new DataIntegrityViolationException("violation", violation);

        ConflictException result = SpaAppointmentService.translateOverlap(e);

        assertThat(result.getMessage()).isEqualTo("This therapist already has an appointment overlapping this time.");
    }

    /** An unrecognized failure must escape untranslated, not get papered over as a generic overlap conflict. */
    @Test
    void translateOverlap_unrecognizedFailure_rethrowsTheOriginalUntranslated() {
        SQLException unrelated = new SQLException("connection failure", "08006");
        DataAccessException e = new DataIntegrityViolationException("unrelated", unrelated);

        assertThatThrownBy(() -> SpaAppointmentService.translateOverlap(e)).isSameAs(e);
    }
}
