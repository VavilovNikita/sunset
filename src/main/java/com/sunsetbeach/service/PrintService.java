package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.repository.PrintJobRepository;
import com.sunsetbeach.repository.PrinterRepository;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The durable print-job queue: every document is written to {@code PrintJob} before delivery is
 * attempted, so a dead/offline/hung printer never silently loses a job and never blocks the
 * order/shift operation that triggered it - the same fail-open contract as {@link EmailService},
 * with a persisted queue and background retry on top instead of a bare try/catch.
 *
 * <p>Documents that are a side effect of a POS/shift action (kitchen/bar tickets, void tickets,
 * guest receipts, the Z-report) go through {@link #queue}: the job is written in the caller's
 * transaction and the first delivery attempt runs on a background thread once that transaction
 * commits. Attempting it in the request thread made every send/void/close wait out the printer's
 * connect timeout (2s) whenever a printer was unreachable. It also means a rolled-back operation
 * no longer prints anything. A print the person is explicitly waiting on (pre-bill, test page,
 * manual retry) still goes through {@link #queueAndAttempt}, whose response reports the outcome.
 */
@Service
public class PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintService.class);

    private final PrinterRepository printerRepository;
    private final PrintJobRepository printJobRepository;
    private final PrinterClient printerClient;
    private final int maxAttempts;
    private final boolean deliverInBackground;
    private final TransactionTemplate deliveryTransaction;
    // One thread: tickets reach a printer in the order they were queued.
    private final ExecutorService deliveryExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "print-delivery");
        t.setDaemon(true);
        return t;
    });

    public PrintService(
            PrinterRepository printerRepository,
            PrintJobRepository printJobRepository,
            PrinterClient printerClient,
            PlatformTransactionManager transactionManager,
            @Value("${app.printing.max-attempts:5}") int maxAttempts,
            @Value("${app.printing.deliver-in-background:true}") boolean deliverInBackground) {
        this.printerRepository = printerRepository;
        this.printJobRepository = printJobRepository;
        this.printerClient = printerClient;
        this.deliveryTransaction = new TransactionTemplate(transactionManager);
        this.maxAttempts = maxAttempts;
        this.deliverInBackground = deliverInBackground;
    }

    @PreDestroy
    void shutDownDelivery() {
        deliveryExecutor.shutdown();
    }

    public Optional<PrinterEntity> findActivePrinter(PrinterDepartment department) {
        return printerRepository.findByDepartmentAndIsActiveTrue(department);
    }

    /** Enqueues the job and attempts delivery once, immediately, in the caller's thread/transaction. Never throws. */
    @Transactional
    public PrintJobEntity queueAndAttempt(
            PrinterEntity printer, PrintDocumentType documentType, String summary, byte[] payload) {
        return attemptAutomatic(insert(printer, documentType, summary, payload), printer);
    }

    /**
     * Enqueues the job in the caller's transaction and returns at once; the first delivery attempt
     * runs on the {@code print-delivery} thread after that transaction commits (see the class
     * javadoc). With {@code app.printing.deliver-in-background=false} it attempts inline instead,
     * which only tests that assert on the delivery outcome inside a rolled-back transaction use.
     */
    @Transactional
    public PrintJobEntity queue(PrinterEntity printer, PrintDocumentType documentType, String summary, byte[] payload) {
        PrintJobEntity job = insert(printer, documentType, summary, payload);
        if (!deliverInBackground) {
            return attemptAutomatic(job, printer);
        }
        String jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliveryExecutor.execute(() -> deliverQueued(jobId));
            }
        });
        return job;
    }

    private PrintJobEntity insert(PrinterEntity printer, PrintDocumentType documentType, String summary, byte[] payload) {
        PrintJobEntity job = new PrintJobEntity();
        job.setPrinterId(printer.getId());
        job.setDocumentType(documentType);
        job.setSummary(summary);
        job.setPayload(payload);
        return printJobRepository.saveAndFlush(job);
    }

    private void deliverQueued(String jobId) {
        try {
            deliveryTransaction.executeWithoutResult(status -> attemptIfStillPending(jobId));
        } catch (Exception e) {
            log.error("Background delivery of PrintJob {} failed; the retry sweep will pick it up", jobId, e);
        }
    }

    /** Locks the job and attempts it only if the other automatic path hasn't already. */
    private void attemptIfStillPending(String jobId) {
        printJobRepository.findByIdForUpdate(jobId).ifPresent(job -> {
            if (job.getStatus() != PrintJobStatus.PENDING || job.getAttempts() >= maxAttempts) {
                return;
            }
            printerRepository
                    .findById(job.getPrinterId())
                    .ifPresentOrElse(
                            printer -> attemptAutomatic(job, printer),
                            () -> log.warn("PrintJob {} references missing printer {}", job.getId(), job.getPrinterId()));
        });
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
            attemptIfStillPending(job.getId());
        }
    }
}
