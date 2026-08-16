package com.sunsetbeach.repository;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.model.PrintJobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintJobRepository extends JpaRepository<PrintJobEntity, String> {

    List<PrintJobEntity> findAllByOrderByCreatedAtDesc();

    List<PrintJobEntity> findByStatusOrderByCreatedAtDesc(PrintJobStatus status);

    /** Candidates for the background retry sweep - jobs still within the attempt budget. */
    List<PrintJobEntity> findByStatusAndAttemptsLessThan(PrintJobStatus status, int maxAttempts);

    /** Gates {@code DELETE /printers/{id}} - a printer that has ever printed anything is kept for history, only deactivated. */
    boolean existsByPrinterId(String printerId);
}
