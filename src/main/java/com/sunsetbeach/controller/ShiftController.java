package com.sunsetbeach.controller;

import com.sunsetbeach.api.ShiftsApi;
import com.sunsetbeach.model.Shift;
import com.sunsetbeach.model.ShiftCloseInput;
import com.sunsetbeach.model.ShiftListItem;
import com.sunsetbeach.model.ShiftOpenInput;
import com.sunsetbeach.model.ShiftSummary;
import com.sunsetbeach.security.CurrentUser;
import com.sunsetbeach.service.ShiftService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShiftController implements ShiftsApi {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @Override
    public ResponseEntity<Shift> openShift(ShiftOpenInput shiftOpenInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.open(CurrentUser.id(), shiftOpenInput));
    }

    @Override
    public ResponseEntity<Shift> getCurrentShift() {
        return ResponseEntity.ok(shiftService.getCurrentOpenShift(CurrentUser.id()));
    }

    @Override
    public ResponseEntity<Shift> closeShift(String id, ShiftCloseInput shiftCloseInput) {
        return ResponseEntity.ok(shiftService.close(id, shiftCloseInput));
    }

    @Override
    public ResponseEntity<ShiftSummary> getShift(String id) {
        return ResponseEntity.ok(shiftService.getSummary(id, CurrentUser.id(), CurrentUser.role()));
    }

    @Override
    public ResponseEntity<List<ShiftListItem>> listShifts(LocalDate from, LocalDate to, String staffId) {
        return ResponseEntity.ok(shiftService.list(from != null ? from.toString() : null, to != null ? to.toString() : null, staffId));
    }

    @Override
    public ResponseEntity<String> exportShift(String id) {
        String csv = shiftService.exportCsv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("shift-" + id + ".csv").build().toString())
                .body(csv);
    }
}
