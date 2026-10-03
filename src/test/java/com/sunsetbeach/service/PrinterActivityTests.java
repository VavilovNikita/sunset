package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.Printer;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.PrinterInput;
import com.sunsetbeach.repository.PrintJobRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code Printer.lastSentAt}/{@code lastFailedAt} - the printers screen's online/offline
 * indicator reads these, so a wrong one tells a manager a dead printer is fine (or the reverse).
 * Both come from the print queue: a still-PENDING job that already failed an attempt counts as a
 * failure, a dismissed one does not (dismissing touches {@code updatedAt}).
 */
@SpringBootTest
@Transactional
class PrinterActivityTests extends AbstractIntegrationTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PrintJobRepository printJobRepository;

    @Autowired
    private EntityManager entityManager;

    private Printer createPrinter() {
        PrinterInput input = new PrinterInput("Activity test printer", PrinterDepartment.KITCHEN, "127.0.0.1");
        input.setIsActive(false); // never collides with a real active KITCHEN printer in the baseline
        return printerService.create(input);
    }

    private void persistJob(String printerId, PrintJobStatus status, String lastError, LocalDateTime updatedAt, boolean dismissed) {
        PrintJobEntity job = new PrintJobEntity();
        job.setPrinterId(printerId);
        job.setDocumentType(PrintDocumentType.TEST_PAGE);
        job.setSummary("test");
        job.setPayload(new byte[] {1});
        job.setStatus(status);
        job.setAttempts(1);
        job.setLastError(lastError);
        job = printJobRepository.saveAndFlush(job);
        // updatedAt is @UpdateTimestamp - backdate it natively, then drop the cached entity.
        entityManager
                .createNativeQuery("UPDATE \"PrintJob\" SET \"updatedAt\" = ?1, \"dismissedAt\" = ?2 WHERE id = ?3")
                .setParameter(1, updatedAt)
                .setParameter(2, dismissed ? updatedAt : null)
                .setParameter(3, job.getId())
                .executeUpdate();
        entityManager.clear();
    }

    private Printer listed(String id) {
        return printerService.list().stream().filter(p -> p.getId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void neverUsedPrinter_hasNeither() {
        Printer printer = createPrinter();

        Printer result = listed(printer.getId());
        assertThat(result.getLastSentAt()).isNull();
        assertThat(result.getLastFailedAt()).isNull();
    }

    @Test
    void newestSentAndNewestUndismissedFailure_areReported() {
        Printer printer = createPrinter();
        persistJob(printer.getId(), PrintJobStatus.SENT, null, T0, false);
        persistJob(printer.getId(), PrintJobStatus.SENT, null, T0.plusHours(1), false);
        // Still PENDING, but its attempt failed - the printer didn't answer, same as FAILED.
        persistJob(printer.getId(), PrintJobStatus.PENDING, "Connect timed out", T0.plusHours(2), false);
        persistJob(printer.getId(), PrintJobStatus.FAILED, "Connect timed out", T0.plusMinutes(30), false);

        Printer result = listed(printer.getId());
        assertThat(result.getLastSentAt()).isEqualTo(T0.plusHours(1).atOffset(ZoneOffset.UTC));
        assertThat(result.getLastFailedAt()).isEqualTo(T0.plusHours(2).atOffset(ZoneOffset.UTC));
    }

    @Test
    void dismissedFailure_doesNotCount() {
        Printer printer = createPrinter();
        persistJob(printer.getId(), PrintJobStatus.SENT, null, T0, false);
        persistJob(printer.getId(), PrintJobStatus.FAILED, "Connect timed out", T0.plusHours(3), true);

        Printer result = listed(printer.getId());
        assertThat(result.getLastSentAt()).isEqualTo(T0.atOffset(ZoneOffset.UTC));
        assertThat(result.getLastFailedAt()).isNull();
    }
}
