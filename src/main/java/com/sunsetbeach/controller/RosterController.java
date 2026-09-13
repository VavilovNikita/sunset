package com.sunsetbeach.controller;

import com.sunsetbeach.api.RosterApi;
import com.sunsetbeach.model.EmployeePattern;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.RosterReassignInput;
import com.sunsetbeach.model.RosterSwapInput;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterLockInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.EmployeePatternService;
import com.sunsetbeach.service.RosterService;
import com.sunsetbeach.service.ShiftCodeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adds the roster grid and its editing surface on top of the shift-code/pattern commit before
 * this one. Coverage, attendance, pay rates, and export still answer 501 (RosterApi's own
 * default) until their own commits add the real implementation.
 */
@RestController
public class RosterController implements RosterApi {

    private final ShiftCodeService shiftCodeService;
    private final EmployeePatternService employeePatternService;
    private final RosterService rosterService;

    public RosterController(ShiftCodeService shiftCodeService, EmployeePatternService employeePatternService, RosterService rosterService) {
        this.shiftCodeService = shiftCodeService;
        this.employeePatternService = employeePatternService;
        this.rosterService = rosterService;
    }

    @Override
    public ResponseEntity<List<ShiftCode>> listShiftCodes(StaffArea staffArea) {
        return ResponseEntity.ok(shiftCodeService.list(staffArea));
    }

    @Override
    public ResponseEntity<ShiftCode> createShiftCode(ShiftCodeCreateInput shiftCodeCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftCodeService.create(shiftCodeCreateInput, callerId()));
    }

    @Override
    public ResponseEntity<List<EmployeePattern>> listEmployeePatterns() {
        return ResponseEntity.ok(employeePatternService.list());
    }

    @Override
    public ResponseEntity<EmployeePattern> setEmployeePattern(String employeeUserId, EmployeePatternInput employeePatternInput) {
        return ResponseEntity.ok(employeePatternService.set(employeeUserId, employeePatternInput, callerId()));
    }

    @Override
    public ResponseEntity<List<RosterEmployee>> listRosterEmployees() {
        return ResponseEntity.ok(rosterService.listEmployees());
    }

    @Override
    public ResponseEntity<RosterMonth> getRosterMonth(Integer year, Integer month) {
        return ResponseEntity.ok(rosterService.getMonth(year, month));
    }

    @Override
    public ResponseEntity<List<RosterEntry>> getMyRoster(Integer year, Integer month) {
        return ResponseEntity.ok(rosterService.getMyRoster(callerId(), year, month));
    }

    @Override
    public ResponseEntity<RosterMonth> generateRosterMonth(Integer year, Integer month) {
        return ResponseEntity.ok(rosterService.generateMonth(year, month, callerId()));
    }

    @Override
    public ResponseEntity<RosterEntry> createRosterEntry(RosterEntryCreateInput rosterEntryCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rosterService.createEntry(rosterEntryCreateInput, callerId()));
    }

    @Override
    public ResponseEntity<OkTrue> deleteRosterEntry(String id) {
        rosterService.deleteEntry(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<RosterEntry> moveRosterEntry(String id, RosterMoveInput rosterMoveInput) {
        return ResponseEntity.ok(rosterService.moveEntry(id, rosterMoveInput, callerId()));
    }

    @Override
    public ResponseEntity<RosterEntry> reassignRosterEntry(String id, RosterReassignInput rosterReassignInput) {
        return ResponseEntity.ok(rosterService.reassignEntry(id, rosterReassignInput, callerId()));
    }

    @Override
    public ResponseEntity<RosterEntry> swapRosterEntries(String id, RosterSwapInput rosterSwapInput) {
        return ResponseEntity.ok(rosterService.swapEntries(id, rosterSwapInput, callerId()));
    }

    @Override
    public ResponseEntity<RosterEntry> setRosterEntryLocked(String id, RosterLockInput rosterLockInput) {
        return ResponseEntity.ok(rosterService.setLocked(id, rosterLockInput.getLocked()));
    }

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
