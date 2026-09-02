package com.sunsetbeach.controller;

import com.sunsetbeach.api.PrintersApi;
import com.sunsetbeach.model.DismissPrintJobsInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.PrintDocumentType;
import com.sunsetbeach.model.PrintJob;
import com.sunsetbeach.model.PrintJobStatus;
import com.sunsetbeach.model.Printer;
import com.sunsetbeach.model.PrinterInput;
import com.sunsetbeach.security.CurrentUser;
import com.sunsetbeach.service.PrinterService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrinterController implements PrintersApi {

    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @Override
    public ResponseEntity<List<Printer>> listPrinters() {
        return ResponseEntity.ok(printerService.list());
    }

    @Override
    public ResponseEntity<Printer> createPrinter(PrinterInput printerInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(printerService.create(printerInput));
    }

    @Override
    public ResponseEntity<Printer> updatePrinter(String id, PrinterInput printerInput) {
        return ResponseEntity.ok(printerService.update(id, printerInput));
    }

    @Override
    public ResponseEntity<OkTrue> deletePrinter(String id) {
        printerService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<PrintJob> testPrinter(String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(printerService.testPrint(id));
    }

    @Override
    public ResponseEntity<List<PrintJob>> listPrintJobs(PrintJobStatus status, PrintDocumentType documentType, Boolean includeDismissed) {
        return ResponseEntity.ok(printerService.listPrintJobs(status, documentType, Boolean.TRUE.equals(includeDismissed), CurrentUser.role()));
    }

    @Override
    public ResponseEntity<List<PrintJob>> dismissPrintJobs(DismissPrintJobsInput dismissPrintJobsInput) {
        return ResponseEntity.ok(
                printerService.dismissPrintJobs(dismissPrintJobsInput.getIds(), dismissPrintJobsInput.getNote(), CurrentUser.role(), CurrentUser.id()));
    }

    @Override
    public ResponseEntity<PrintJob> retryPrintJob(String id) {
        return ResponseEntity.ok(printerService.retryPrintJob(id, CurrentUser.role()));
    }

    @Override
    public ResponseEntity<String> previewPrintJob(String id) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(printerService.previewPrintJob(id, CurrentUser.role()));
    }
}
