package com.sunsetbeach.controller;

import com.sunsetbeach.api.SpaApi;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaMap;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.SpaTherapist;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.SpaAppointmentService;
import com.sunsetbeach.service.SpaMapService;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SpaController implements SpaApi {

    private final SpaAppointmentService spaAppointmentService;
    private final SpaMapService spaMapService;

    public SpaController(SpaAppointmentService spaAppointmentService, SpaMapService spaMapService) {
        this.spaAppointmentService = spaAppointmentService;
        this.spaMapService = spaMapService;
    }

    @Override
    public ResponseEntity<SpaSchedule> getSpaSchedule(String date) {
        return ResponseEntity.ok(spaAppointmentService.getSchedule(LocalDate.parse(date)));
    }

    @Override
    public ResponseEntity<List<SpaTherapist>> listSpaTherapists() {
        return ResponseEntity.ok(spaAppointmentService.listTherapists());
    }

    @Override
    public ResponseEntity<SpaAppointmentResult> createSpaAppointment(SpaAppointmentCreateInput spaAppointmentCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spaAppointmentService.create(spaAppointmentCreateInput, callerId()));
    }

    @Override
    public ResponseEntity<SpaAppointment> updateSpaAppointmentStatus(String id, SpaAppointmentStatusUpdateInput spaAppointmentStatusUpdateInput) {
        return ResponseEntity.ok(spaAppointmentService.updateStatus(id, spaAppointmentStatusUpdateInput, callerId()));
    }

    @Override
    public ResponseEntity<SpaMap> getSpaMap() {
        return ResponseEntity.ok(spaMapService.get());
    }

    @Override
    public ResponseEntity<Resource> getSpaMapImage() {
        Resource resource = spaMapService.resolveImage();
        MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(contentType)
                // Same reasoning as PropertyMapController#getPropertyMapImage - short-lived, not
                // immutable: this URL has no filename in it, so the same URL legitimately serves
                // different bytes after a manager replaces the plan. The frontend appends
                // ?v=<imageUpdatedAt> to force a fresh fetch on replacement.
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(resource);
    }

    @Override
    public ResponseEntity<SpaMap> uploadSpaMapImage(MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spaMapService.uploadImage(file));
    }

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
