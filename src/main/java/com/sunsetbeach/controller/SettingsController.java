package com.sunsetbeach.controller;

import com.sunsetbeach.api.SettingsApi;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.model.NotificationSettings;
import com.sunsetbeach.model.StaffBotSettings;
import com.sunsetbeach.model.StaffBotSettingsUpdateInput;
import com.sunsetbeach.service.StaffBotSettingsService;
import com.sunsetbeach.model.NotificationSettingsUpdateInput;
import com.sunsetbeach.model.PrintAttemptResult;
import com.sunsetbeach.model.PrintLayout;
import com.sunsetbeach.model.PrintLayoutDocument;
import com.sunsetbeach.model.PrintLayoutInput;
import com.sunsetbeach.model.PrintLayoutPreview;
import com.sunsetbeach.model.PrintLayouts;
import com.sunsetbeach.model.VatSettings;
import com.sunsetbeach.model.VatSettingsUpdateInput;
import com.sunsetbeach.service.LifecycleEmailSettingsService;
import com.sunsetbeach.service.ManualUpdateService;
import com.sunsetbeach.service.NotificationSettingsService;
import com.sunsetbeach.service.PrintLayoutService;
import com.sunsetbeach.service.VatSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SettingsController implements SettingsApi {

    private final LifecycleEmailSettingsService lifecycleEmailSettingsService;
    private final VatSettingsService vatSettingsService;
    private final com.sunsetbeach.service.ServiceChargeSettingsService serviceChargeSettingsService;
    private final PrintLayoutService printLayoutService;
    private final NotificationSettingsService notificationSettingsService;
    private final StaffBotSettingsService staffBotSettingsService;
    private final ManualUpdateService manualUpdateService;

    public SettingsController(
            LifecycleEmailSettingsService lifecycleEmailSettingsService,
            VatSettingsService vatSettingsService,
            com.sunsetbeach.service.ServiceChargeSettingsService serviceChargeSettingsService,
            PrintLayoutService printLayoutService,
            NotificationSettingsService notificationSettingsService,
            StaffBotSettingsService staffBotSettingsService,
            ManualUpdateService manualUpdateService) {
        this.manualUpdateService = manualUpdateService;
        this.staffBotSettingsService = staffBotSettingsService;
        this.notificationSettingsService = notificationSettingsService;
        this.lifecycleEmailSettingsService = lifecycleEmailSettingsService;
        this.vatSettingsService = vatSettingsService;
        this.serviceChargeSettingsService = serviceChargeSettingsService;
        this.printLayoutService = printLayoutService;
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
    public ResponseEntity<NotificationSettings> getNotificationSettings() {
        return ResponseEntity.ok(notificationSettingsService.get());
    }

    @Override
    public ResponseEntity<NotificationSettings> updateNotificationSettings(NotificationSettingsUpdateInput input) {
        boolean manualUpdatesWereOn = notificationSettingsService.getEntity().isManualUpdatesEnabled();
        NotificationSettings saved = notificationSettingsService.update(input);
        // Switched back on: what was published meanwhile is not sent later (the sweep may not have seen it yet).
        if (!manualUpdatesWereOn && Boolean.TRUE.equals(saved.getManualUpdatesEnabled())) {
            manualUpdateService.skipPublished();
        }
        return ResponseEntity.ok(saved);
    }

    @Override
    public ResponseEntity<StaffBotSettings> getStaffBotSettings() {
        return ResponseEntity.ok(staffBotSettingsService.get());
    }

    @Override
    public ResponseEntity<StaffBotSettings> updateStaffBotSettings(StaffBotSettingsUpdateInput input) {
        return ResponseEntity.ok(staffBotSettingsService.update(input));
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
    public ResponseEntity<com.sunsetbeach.model.ServiceChargeSettings> getServiceChargeSettings() {
        return ResponseEntity.ok(serviceChargeSettingsService.get());
    }

    @Override
    public ResponseEntity<com.sunsetbeach.model.ServiceChargeSettings> updateServiceChargeSettings(
            com.sunsetbeach.model.ServiceChargeSettingsUpdateInput input) {
        return ResponseEntity.ok(serviceChargeSettingsService.update(input));
    }

    @Override
    public ResponseEntity<PrintLayouts> getPrintLayouts() {
        return ResponseEntity.ok(printLayoutService.list());
    }

    @Override
    public ResponseEntity<PrintLayout> updatePrintLayout(PrintLayoutDocument documentType, PrintLayoutInput input) {
        return ResponseEntity.ok(printLayoutService.update(documentType, input));
    }

    @Override
    public ResponseEntity<PrintLayoutPreview> previewPrintLayout(PrintLayoutDocument documentType, PrintLayoutInput input) {
        return ResponseEntity.ok(printLayoutService.preview(documentType, input));
    }

    @Override
    public ResponseEntity<PrintLayouts> uploadPrintLogo(MultipartFile file) {
        return ResponseEntity.ok(printLayoutService.setLogo(file));
    }

    @Override
    public ResponseEntity<PrintLayouts> deletePrintLogo() {
        return ResponseEntity.ok(printLayoutService.removeLogo());
    }

    @Override
    public ResponseEntity<PrintAttemptResult> testPrintLayout(PrintLayoutDocument documentType) {
        return ResponseEntity.status(201).body(printLayoutService.testPrint(documentType));
    }
}
