package com.sunsetbeach.repository;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.model.PrintJobStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrintJobRepository extends JpaRepository<PrintJobEntity, String> {

    List<PrintJobEntity> findAllByOrderByCreatedAtDesc();

    List<PrintJobEntity> findByStatusOrderByCreatedAtDesc(PrintJobStatus status);

    /** Candidates for the background retry sweep - jobs still within the attempt budget. */
    List<PrintJobEntity> findByStatusAndAttemptsLessThan(PrintJobStatus status, int maxAttempts);

    /**
     * Newest job update per printer among jobs in {@code status} - with {@code SENT}, that's
     * {@code Printer.lastSentAt}. Rows are {@code [printerId, max(updatedAt)]}. The status is a
     * bound parameter, not a JPQL enum literal: Hibernate renders a literal as a cast to an
     * unquoted type name, which misses the quoted native enum.
     */
    @Query("select j.printerId, max(j.updatedAt) from PrintJobEntity j where j.status = :status group by j.printerId")
    List<Object[]> findLastUpdatedAtByPrinter(@Param("status") PrintJobStatus status);

    /**
     * Newest failed delivery attempt per printer - {@code Printer.lastFailedAt}. A job still
     * PENDING after a failed attempt counts as much as a FAILED one (it is the same "printer
     * didn't answer"); dismissed jobs don't, because dismissing touches {@code updatedAt} and
     * would otherwise read as a fresh failure.
     */
    @Query("select j.printerId, max(j.updatedAt) from PrintJobEntity j"
            + " where j.status <> :sent and j.lastError is not null and j.dismissedAt is null"
            + " group by j.printerId")
    List<Object[]> findLastFailedAtByPrinter(@Param("sent") PrintJobStatus sent);

    /**
     * Taken by every automatic delivery attempt (the after-commit one and the background sweep)
     * before re-checking the job is still {@code PENDING}, so the two can never both send the same
     * ticket.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from PrintJobEntity j where j.id = :id")
    Optional<PrintJobEntity> findByIdForUpdate(@Param("id") String id);

    /** Gates {@code DELETE /printers/{id}} - a printer that has ever printed anything is kept for history, only deactivated. */
    boolean existsByPrinterId(String printerId);
}
