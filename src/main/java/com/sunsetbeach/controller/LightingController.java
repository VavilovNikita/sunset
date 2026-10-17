package com.sunsetbeach.controller;

import com.sunsetbeach.api.LightingApi;
import com.sunsetbeach.model.LightingDevice;
import com.sunsetbeach.model.LightingDeviceCheckInput;
import com.sunsetbeach.model.LightingDeviceCheckResult;
import com.sunsetbeach.model.LightingDeviceConfig;
import com.sunsetbeach.model.LightingDeviceCreateInput;
import com.sunsetbeach.model.LightingDeviceList;
import com.sunsetbeach.model.LightingDeviceUpdateInput;
import com.sunsetbeach.model.LightingReorderInput;
import com.sunsetbeach.model.LightingStateInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.service.LightingService;
import com.sunsetbeach.service.LightingSettingsService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LightingController implements LightingApi {

    private final LightingService lightingService;
    private final LightingSettingsService settingsService;

    public LightingController(LightingService lightingService, LightingSettingsService settingsService) {
        this.lightingService = lightingService;
        this.settingsService = settingsService;
    }

    @Override
    public ResponseEntity<LightingDeviceList> listLightingDevices(Boolean refresh) {
        return ResponseEntity.ok(lightingService.list(Boolean.TRUE.equals(refresh)));
    }

    @Override
    public ResponseEntity<LightingDevice> setLightingState(String id, LightingStateInput lightingStateInput) {
        return ResponseEntity.ok(lightingService.setState(id, lightingStateInput));
    }

    @Override
    public ResponseEntity<List<LightingDeviceConfig>> listLightingDeviceConfigs() {
        return ResponseEntity.ok(settingsService.list());
    }

    @Override
    public ResponseEntity<LightingDeviceConfig> createLightingDeviceConfig(LightingDeviceCreateInput lightingDeviceCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settingsService.create(lightingDeviceCreateInput));
    }

    @Override
    public ResponseEntity<LightingDeviceCheckResult> checkLightingDevice(LightingDeviceCheckInput lightingDeviceCheckInput) {
        return ResponseEntity.ok(settingsService.check(lightingDeviceCheckInput));
    }

    @Override
    public ResponseEntity<List<LightingDeviceConfig>> reorderLightingDeviceConfigs(LightingReorderInput lightingReorderInput) {
        return ResponseEntity.ok(settingsService.reorder(lightingReorderInput.getIds()));
    }

    @Override
    public ResponseEntity<LightingDeviceConfig> updateLightingDeviceConfig(String id, LightingDeviceUpdateInput lightingDeviceUpdateInput) {
        return ResponseEntity.ok(settingsService.update(id, lightingDeviceUpdateInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteLightingDeviceConfig(String id) {
        settingsService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
