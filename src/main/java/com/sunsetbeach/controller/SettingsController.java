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
    private final PrintLayoutService printLayoutService;
    private final NotificationSettingsService notificationSettingsService;
    private final StaffBotSettingsService staffBotSettingsService;

    public SettingsController(
            LifecycleEmailSettingsService lifecycleEmailSettingsService,
            VatSettingsService vatSettingsService,
            PrintLayoutService printLayoutService,
            NotificationSettingsService notificationSettingsService,
            StaffBotSettingsService staffBotSettingsService) {
        this.staffBotSettingsService = staffBotSettingsService;
        this.notificationSettingsService = notificationSettingsService;
        this.lifecycleEmailSettingsService = lifecycleEmailSettingsService;
        this.vatSettingsService = vatSettingsService;
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
        return ResponseEntity.ok(notificationSettingsService.update(input));
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
