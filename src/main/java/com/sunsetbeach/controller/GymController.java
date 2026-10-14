package com.sunsetbeach.controller;

import com.sunsetbeach.api.GymApi;
import com.sunsetbeach.model.GymAcCommandInput;
import com.sunsetbeach.model.GymDevice;
import com.sunsetbeach.model.GymDeviceConfig;
import com.sunsetbeach.model.GymDeviceConfigInput;
import com.sunsetbeach.model.GymDeviceList;
import com.sunsetbeach.model.GymRelayInput;
import com.sunsetbeach.model.GymReorderInput;
import com.sunsetbeach.model.GymStatus;
import com.sunsetbeach.model.GymTuyaUsage;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.TuyaDiscoveredDevice;
import com.sunsetbeach.model.TuyaRemote;
import com.sunsetbeach.service.GymDeviceService;
import com.sunsetbeach.service.GymSettingsService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GymController implements GymApi {

    private final GymDeviceService gymDeviceService;
    private final GymSettingsService gymSettingsService;

    public GymController(GymDeviceService gymDeviceService, GymSettingsService gymSettingsService) {
        this.gymDeviceService = gymDeviceService;
        this.gymSettingsService = gymSettingsService;
    }

    @Override
    public ResponseEntity<GymDeviceList> listGymDevices(Boolean refresh) {
        return ResponseEntity.ok(gymDeviceService.list(Boolean.TRUE.equals(refresh)));
    }

    @Override
    public ResponseEntity<GymStatus> getGymStatus() {
        return ResponseEntity.ok(gymDeviceService.status());
    }

    @Override
    public ResponseEntity<GymDevice> setGymRelay(String id, GymRelayInput gymRelayInput) {
        return ResponseEntity.ok(gymDeviceService.setRelay(id, gymRelayInput.getIsOn()));
    }

    @Override
    public ResponseEntity<GymDevice> sendGymAcCommand(String id, GymAcCommandInput gymAcCommandInput) {
        return ResponseEntity.ok(gymDeviceService.sendAcCommand(id, gymAcCommandInput));
    }

    @Override
    public ResponseEntity<List<GymDeviceConfig>> listGymDeviceConfigs() {
        return ResponseEntity.ok(gymSettingsService.list());
    }

    @Override
    public ResponseEntity<GymDeviceConfig> createGymDeviceConfig(GymDeviceConfigInput gymDeviceConfigInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymSettingsService.create(gymDeviceConfigInput));
    }

    @Override
    public ResponseEntity<List<GymDeviceConfig>> reorderGymDeviceConfigs(GymReorderInput gymReorderInput) {
        return ResponseEntity.ok(gymSettingsService.reorder(gymReorderInput.getIds()));
    }

    @Override
    public ResponseEntity<GymDeviceConfig> updateGymDeviceConfig(String id, GymDeviceConfigInput gymDeviceConfigInput) {
        return ResponseEntity.ok(gymSettingsService.update(id, gymDeviceConfigInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteGymDeviceConfig(String id) {
        gymSettingsService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<List<TuyaDiscoveredDevice>> listTuyaDevices() {
        return ResponseEntity.ok(gymSettingsService.listTuyaDevices());
    }

    @Override
    public ResponseEntity<List<String>> listTuyaSwitchCodes(String tuyaDeviceId) {
        return ResponseEntity.ok(gymSettingsService.listSwitchCodes(tuyaDeviceId));
    }

    @Override
    public ResponseEntity<List<TuyaRemote>> listTuyaInfraredRemotes(String infraredId) {
        return ResponseEntity.ok(gymSettingsService.listRemotes(infraredId));
    }

    @Override
    public ResponseEntity<GymTuyaUsage> getGymTuyaUsage() {
        return ResponseEntity.ok(gymSettingsService.usage());
    }
}
