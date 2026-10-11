package com.sunsetbeach.controller;

import com.sunsetbeach.api.SettingsApi;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.ReceiptPreview;
import com.sunsetbeach.model.ReceiptSettings;
import com.sunsetbeach.model.ReceiptSettingsInput;
import com.sunsetbeach.model.VatSettings;
import com.sunsetbeach.model.VatSettingsUpdateInput;
import com.sunsetbeach.service.LifecycleEmailSettingsService;
import com.sunsetbeach.service.ReceiptSettingsService;
import com.sunsetbeach.service.VatSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SettingsController implements SettingsApi {

    private final LifecycleEmailSettingsService lifecycleEmailSettingsService;
    private final VatSettingsService vatSettingsService;
    private final ReceiptSettingsService receiptSettingsService;

    public SettingsController(
            LifecycleEmailSettingsService lifecycleEmailSettingsService,
            VatSettingsService vatSettingsService,
            ReceiptSettingsService receiptSettingsService) {
        this.lifecycleEmailSettingsService = lifecycleEmailSettingsService;
        this.vatSettingsService = vatSettingsService;
        this.receiptSettingsService = receiptSettingsService;
    }

    @Override
    public ResponseEntity<LifecycleEmailSettings> getLifecycleEmailSettings() {
        return ResponseEntity.ok(lifecycleEmailSettingsService.get());
    }

    @Override
    public ResponseEntity<LifecycleEmailSettings> updateLifecycleEmailSettings(LifecycleEmailSettingsUpdateInput input) {
        return ResponseEntity.ok(lifecycleEmailSettingsService.update(input));
    }

    @Override
    public ResponseEntity<VatSettings> getVatSettings() {
        return ResponseEntity.ok(vatSettingsService.get());
    }

    @Override
    public ResponseEntity<VatSettings> updateVatSettings(VatSettingsUpdateInput input) {
        return ResponseEntity.ok(vatSettingsService.update(input));
    }

    @Override
    public ResponseEntity<ReceiptSettings> getReceiptSettings() {
        return ResponseEntity.ok(receiptSettingsService.get());
    }

    @Override
    public ResponseEntity<ReceiptSettings> updateReceiptSettings(ReceiptSettingsInput input) {
        return ResponseEntity.ok(receiptSettingsService.update(input));
    }

    @Override
    public ResponseEntity<ReceiptPreview> previewReceipt(ReceiptSettingsInput input) {
        return ResponseEntity.ok(receiptSettingsService.preview(input));
    }

    @Override
    public ResponseEntity<ReceiptSettings> uploadReceiptLogo(MultipartFile file) {
        return ResponseEntity.ok(receiptSettingsService.setLogo(file));
    }

    @Override
    public ResponseEntity<ReceiptSettings> deleteReceiptLogo() {
        return ResponseEntity.ok(receiptSettingsService.removeLogo());
    }

    @Override
    public ResponseEntity<PrintAttemptResult> testPrintReceipt() {
        return ResponseEntity.status(201).body(receiptSettingsService.testPrint());
    }
}
