package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.repository.PrintJobRepository;
import com.sunsetbeach.repository.PrinterRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The durable print-job queue: every document is written to {@code PrintJob} before delivery is
 * attempted, so a dead/offline/hung printer never silently loses a job and never blocks the
 * order/shift operation that triggered it - the same fail-open contract as {@link EmailService},
 * with a persisted queue and background retry on top instead of a bare try/catch.
 */
@Service
public class PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintService.class);

    private final PrinterRepository printerRepository;
    private final PrintJobRepository printJobRepository;
    private final PrinterClient printerClient;
    private final int maxAttempts;

    public PrintService(
            PrinterRepository printerRepository,
            PrintJobRepository printJobRepository,
            PrinterClient printerClient,
            @Value("${app.printing.max-attempts:5}") int maxAttempts) {
        this.printerRepository = printerRepository;
        this.printJobRepository = printJobRepository;
        this.printerClient = printerClient;
        this.maxAttempts = maxAttempts;
    }

    public Optional<PrinterEntity> findActivePrinter(PrinterDepartment department) {
        return printerRepository.findByDepartmentAndIsActiveTrue(department);
    }

    /** Enqueues the job and attempts delivery once, immediately, in the caller's thread/transaction. Never throws. */
    @Transactional
    public PrintJobEntity queueAndAttempt(
            PrinterEntity printer, PrintDocumentType documentType, String summary, byte[] payload) {
        PrintJobEntity job = new PrintJobEntity();
        job.setPrinterId(printer.getId());
        job.setDocumentType(documentType);
        job.setSummary(summary);
        job.setPayload(payload);
        job = printJobRepository.saveAndFlush(job);
        return attemptAutomatic(job, printer);
    }

    /**
     * {@code POST /print-jobs/{id}/retry} - a human asking for a retry overrides the automatic
     * attempt ceiling, so unlike {@link #attemptAutomatic}, the outcome is always terminal
     * ({@code SENT} or {@code FAILED}), never left {@code PENDING} for another automatic pass.
     */
    @Transactional
    public PrintJobEntity retryManually(PrintJobEntity job, PrinterEntity printer) {
        job.setAttempts(job.getAttempts() + 1);
        try {
            printerClient.send(printer.getHost(), printer.getPort(), job.getPayload());
            markSent(job);
        } catch (Exception e) {
            markFailed(job, e);
        }
        return printJobRepository.saveAndFlush(job);
    }

    /**
     * On success -> {@code SENT}. On failure, stays {@code PENDING} (eligible for the next
     * background sweep) until {@code maxAttempts} is reached, then becomes {@code FAILED}.
     */
    private PrintJobEntity attemptAutomatic(PrintJobEntity job, PrinterEntity printer) {
        job.setAttempts(job.getAttempts() + 1);
        try {
            printerClient.send(printer.getHost(), printer.getPort(), job.getPayload());
            markSent(job);
        } catch (Exception e) {
            if (job.getAttempts() >= maxAttempts) {
                markFailed(job, e);
            } else {
                job.setStatus(PrintJobStatus.PENDING);
                job.setLastError(describe(e));
            }
        }
        return printJobRepository.saveAndFlush(job);
    }

    private static void markSent(PrintJobEntity job) {
        job.setStatus(PrintJobStatus.SENT);
        job.setLastError(null);
    }

    private static void markFailed(PrintJobEntity job, Exception e) {
        job.setStatus(PrintJobStatus.FAILED);
        job.setLastError(describe(e));
    }

    private static String describe(Exception e) {
        String message = e.getMessage();
        return message != null && !message.isBlank() ? message : e.getClass().getSimpleName();
    }

    /**
     * Background retry sweep for jobs still {@code PENDING} and under the attempt budget - the
     * other half of the fail-open contract: a printer that comes back online eventually gets its
     * backlog delivered without a human touching {@code POST /print-jobs/{id}/retry}. Each job is
     * attempted independently so one stuck/slow printer doesn't delay the rest of the sweep.
     */
    @Scheduled(fixedDelayString = "${app.printing.retry-interval-ms:60000}")
    @Transactional
    public void retryPendingJobs() {
        List<PrintJobEntity> candidates =
                printJobRepository.findByStatusAndAttemptsLessThan(PrintJobStatus.PENDING, maxAttempts);
        for (PrintJobEntity job : candidates) {
            printerRepository
                    .findById(job.getPrinterId())
                    .ifPresentOrElse(
                            printer -> attemptAutomatic(job, printer),
                            () -> log.warn("PrintJob {} references missing printer {}", job.getId(), job.getPrinterId()));
        }
    }
}
