package com.sunsetbeach.controller;

import com.sunsetbeach.api.SettingsApi;
import com.sunsetbeach.model.LifecycleEmailSettings;
import com.sunsetbeach.model.LifecycleEmailSettingsUpdateInput;
import com.sunsetbeach.service.LifecycleEmailSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController implements SettingsApi {

    private final LifecycleEmailSettingsService lifecycleEmailSettingsService;

    public SettingsController(LifecycleEmailSettingsService lifecycleEmailSettingsService) {
        this.lifecycleEmailSettingsService = lifecycleEmailSettingsService;
    }

    @Override
    public ResponseEntity<LifecycleEmailSettings> getLifecycleEmailSettings() {
        return ResponseEntity.ok(lifecycleEmailSettingsService.get());
    }

    @Override
    public ResponseEntity<LifecycleEmailSettings> updateLifecycleEmailSettings(LifecycleEmailSettingsUpdateInput input) {
        return ResponseEntity.ok(lifecycleEmailSettingsService.update(input));
    }
}
