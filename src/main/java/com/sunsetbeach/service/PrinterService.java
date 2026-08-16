package com.sunsetbeach.service;

import com.sunsetbeach.entity.PrintJobEntity;
import com.sunsetbeach.entity.PrinterEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.PrintJobMapper;
import com.sunsetbeach.mapper.PrinterMapper;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJob;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.Printer;
import com.sunsetbeach.model.PrinterCodepage;
import com.sunsetbeach.model.PrinterDepartment;
import com.sunsetbeach.model.PrinterInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.PrintJobRepository;
import com.sunsetbeach.repository.PrinterRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrinterService {

    /**
     * What CASHIER/WAITER may see in the print-job queue - kitchen/bar tickets and pre-bills
     * only. Z-reports (shift cash reconciliation) and guest receipts (booking/payment detail)
     * are cashier/management information, not something line staff should be able to browse or
     * force a reprint of by guessing a job id. ADMIN/MANAGER see every document type - see
     * {@link #isVisible}.
     */
    private static final Set<PrintDocumentType> STAFF_VISIBLE_DOCUMENT_TYPES =
            EnumSet.of(PrintDocumentType.KITCHEN_TICKET, PrintDocumentType.PREBILL);

    private final PrinterRepository printerRepository;
    private final PrintJobRepository printJobRepository;
    private final PrinterMapper printerMapper;
    private final PrintJobMapper printJobMapper;
    private final PrintService printService;

    public PrinterService(
            PrinterRepository printerRepository,
            PrintJobRepository printJobRepository,
            PrinterMapper printerMapper,
            PrintJobMapper printJobMapper,
            PrintService printService) {
        this.printerRepository = printerRepository;
        this.printJobRepository = printJobRepository;
        this.printerMapper = printerMapper;
        this.printJobMapper = printJobMapper;
        this.printService = printService;
    }

    @Transactional(readOnly = true)
    public List<Printer> list() {
        return printerRepository.findAll().stream().map(printerMapper::toDto).toList();
    }

    @Transactional
    public Printer create(PrinterInput input) {
        if (isActive(input)) {
            requireNoActiveConflict(input.getDepartment(), null);
        }
        PrinterEntity entity = new PrinterEntity();
        printerMapper.applyInput(entity, input);
        try {
            return printerMapper.toDto(printerRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            // Belt-and-suspenders: the unique partial index (Printer_one_active_per_department)
            // is the real guard against a race between two concurrent activations.
            throw conflictFor(input.getDepartment());
        }
    }

    @Transactional
    public Printer update(String id, PrinterInput input) {
        PrinterEntity entity = findOrThrow(id);
        if (isActive(input)) {
            requireNoActiveConflict(input.getDepartment(), id);
        }
        printerMapper.applyInput(entity, input);
        try {
            return printerMapper.toDto(printerRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw conflictFor(input.getDepartment());
        }
    }

    @Transactional
    public void delete(String id) {
        PrinterEntity entity = findOrThrow(id);
        if (printJobRepository.existsByPrinterId(id)) {
            throw new ConflictException("This printer has existing print jobs and can't be deleted.");
        }
        printerRepository.delete(entity);
    }

    @Transactional
    public PrintJob testPrint(String id) {
        PrinterEntity printer = findOrThrow(id);
        byte[] payload = buildTestPagePayload(printer.getCodepage());
        PrintJobEntity job =
                printService.queueAndAttempt(printer, PrintDocumentType.TEST_PAGE, "Test page — " + printer.getName(), payload);
        return printJobMapper.toDto(job);
    }

    @Transactional(readOnly = true)
    public List<PrintJob> listPrintJobs(PrintJobStatus status, PrintDocumentType documentType, Role callerRole) {
        List<PrintJobEntity> jobs = status != null
                ? printJobRepository.findByStatusOrderByCreatedAtDesc(status)
                : printJobRepository.findAllByOrderByCreatedAtDesc();
        return jobs.stream()
                .filter(job -> isVisible(job.getDocumentType(), callerRole))
                .filter(job -> documentType == null || job.getDocumentType() == documentType)
                .map(printJobMapper::toDto)
                .toList();
    }

    @Transactional
    public PrintJob retryPrintJob(String id, Role callerRole) {
        PrintJobEntity job = findVisibleOrThrow(id, callerRole);
        // No ON DELETE on PrintJob.printerId (see V5 migration) - the printer this job targets
        // always still exists, deactivated or not.
        PrinterEntity printer = printerRepository
                .findById(job.getPrinterId())
                .orElseThrow(() -> new NotFoundException("Printer not found"));
        return printJobMapper.toDto(printService.retryManually(job, printer));
    }

    /**
     * {@code GET /print-jobs/{id}/preview} - the same text that would come out of the printer,
     * with the ESC/POS control sequences (init, code page, bold, centering, cut) stripped out.
     * Lets staff check a document before it prints, or see what a FAILED job actually contained
     * without walking over to a dead printer.
     */
    @Transactional(readOnly = true)
    public String previewPrintJob(String id, Role callerRole) {
        PrintJobEntity job = findVisibleOrThrow(id, callerRole);
        PrinterEntity printer = printerRepository
                .findById(job.getPrinterId())
                .orElseThrow(() -> new NotFoundException("Printer not found"));
        return EscPosPreview.render(job.getPayload(), printer.getCodepage());
    }

    /**
     * Shared by {@link #retryPrintJob} and {@link #previewPrintJob}: 404, not 403, for a job
     * outside the caller's visible set - it must look identical to a job that doesn't exist, or
     * the id could be used to confirm/probe for (or force a reprint/read the content of) e.g. a
     * Z-report.
     */
    private PrintJobEntity findVisibleOrThrow(String id, Role callerRole) {
        PrintJobEntity job =
                printJobRepository.findById(id).orElseThrow(() -> new NotFoundException("Print job not found"));
        if (!isVisible(job.getDocumentType(), callerRole)) {
            throw new NotFoundException("Print job not found");
        }
        return job;
    }

    /**
     * Deliberately not {@code Role#ordinal()} - the enum's declaration order mirrors
     * openapi.yaml's listing order and isn't a contract; comparing against the named constants
     * keeps this correct even if the spec's role list is ever reordered.
     */
    private static boolean isVisible(PrintDocumentType type, Role callerRole) {
        if (callerRole == Role.ADMIN || callerRole == Role.MANAGER) {
            return true;
        }
        return STAFF_VISIBLE_DOCUMENT_TYPES.contains(type);
    }

    private static boolean isActive(PrinterInput input) {
        return input.getIsActive() == null || input.getIsActive();
    }

    private void requireNoActiveConflict(PrinterDepartment department, String excludeId) {
        printerRepository
                .findByDepartmentAndIsActiveTrue(department)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw conflictFor(department);
                });
    }

    private static ConflictException conflictFor(PrinterDepartment department) {
        return new ConflictException("Another printer is already active for department " + department.getValue());
    }

    private static byte[] buildTestPagePayload(PrinterCodepage codepage) {
        return new EscPosBuilder(codepage)
                .center(true)
                .bold(true)
                .line("TEST PAGE")
                .bold(false)
                .line(TimestampFormat.readable(LocalDateTime.now()))
                .center(false)
                .divider()
                .line("If you can read this, the host, port, and codepage are configured correctly.")
                .cutAndBuild();
    }

    private PrinterEntity findOrThrow(String id) {
        return printerRepository.findById(id).orElseThrow(() -> new NotFoundException("Printer not found"));
    }
}
