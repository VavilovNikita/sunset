package com.sunsetbeach.controller;

import com.sunsetbeach.api.MaintenanceTasksApi;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskBlockResult;
import com.sunsetbeach.model.MaintenanceTaskStatusUpdateInput;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.MaintenanceTaskService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MaintenanceTaskController implements MaintenanceTasksApi {

    private final MaintenanceTaskService maintenanceTaskService;

    public MaintenanceTaskController(MaintenanceTaskService maintenanceTaskService) {
        this.maintenanceTaskService = maintenanceTaskService;
    }

    @Override
    public ResponseEntity<List<MaintenanceTask>> listMaintenanceTasks() {
        return ResponseEntity.ok(maintenanceTaskService.list());
    }

    @Override
    public ResponseEntity<MaintenanceTask> createMaintenanceTask(String roomUnitId, String description, List<MultipartFile> photos) {
        String callerId = ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceTaskService.create(roomUnitId, description, photos, callerId));
    }

    @Override
    public ResponseEntity<Resource> getMaintenanceTaskPhoto(String id, String filename) {
        Resource resource = maintenanceTaskService.resolvePhoto(id, filename);
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(contentType).body(resource);
    }

    @Override
    public ResponseEntity<MaintenanceTaskBlockResult> blockMaintenanceTaskRoom(String id, RoomUnitBlockInput roomUnitBlockInput) {
        return ResponseEntity.ok(maintenanceTaskService.addBlock(id, roomUnitBlockInput));
    }

    @Override
    public ResponseEntity<MaintenanceTask> updateMaintenanceTaskStatus(String id, MaintenanceTaskStatusUpdateInput maintenanceTaskStatusUpdateInput) {
        return ResponseEntity.ok(maintenanceTaskService.updateStatus(id, maintenanceTaskStatusUpdateInput.getStatus()));
    }
}
