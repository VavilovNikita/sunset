package com.sunsetbeach.controller;

import com.sunsetbeach.api.SettingsApi;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.model.VatSettings;
import com.sunsetbeach.model.VatSettingsUpdateInput;
import com.sunsetbeach.service.LifecycleEmailSettingsService;
import com.sunsetbeach.service.VatSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController implements SettingsApi {

    private final LifecycleEmailSettingsService lifecycleEmailSettingsService;
    private final VatSettingsService vatSettingsService;

    public SettingsController(LifecycleEmailSettingsService lifecycleEmailSettingsService, VatSettingsService vatSettingsService) {
        this.lifecycleEmailSettingsService = lifecycleEmailSettingsService;
        this.vatSettingsService = vatSettingsService;
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
}
