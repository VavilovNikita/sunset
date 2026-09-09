package com.sunsetbeach.controller;

import com.sunsetbeach.api.SpaApi;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentResult;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.SpaSchedule;
import com.sunsetbeach.model.SpaTherapist;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.SpaAppointmentService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpaController implements SpaApi {

    private final SpaAppointmentService spaAppointmentService;

    public SpaController(SpaAppointmentService spaAppointmentService) {
        this.spaAppointmentService = spaAppointmentService;
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

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
