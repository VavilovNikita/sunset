package com.sunsetbeach.controller;

import com.sunsetbeach.api.RosterApi;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
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
import com.sunsetbeach.model.StaffAreaCoverageRule;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.AttendanceService;
import com.sunsetbeach.service.EmployeePatternService;
import com.sunsetbeach.service.RosterService;
import com.sunsetbeach.service.ShiftCodeService;
import com.sunsetbeach.service.StaffAreaCoverageRuleService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adds attendance on top of the coverage commit before this one - pay rates and export still
 * answer 501 (RosterApi's own default) until their own commit.
 */
@RestController
public class RosterController implements RosterApi {

    private final ShiftCodeService shiftCodeService;
    private final EmployeePatternService employeePatternService;
    private final RosterService rosterService;
    private final StaffAreaCoverageRuleService staffAreaCoverageRuleService;
    private final AttendanceService attendanceService;

    public RosterController(
            ShiftCodeService shiftCodeService,
            EmployeePatternService employeePatternService,
            RosterService rosterService,
            StaffAreaCoverageRuleService staffAreaCoverageRuleService,
            AttendanceService attendanceService) {
        this.shiftCodeService = shiftCodeService;
        this.employeePatternService = employeePatternService;
        this.rosterService = rosterService;
        this.staffAreaCoverageRuleService = staffAreaCoverageRuleService;
        this.attendanceService = attendanceService;
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

    @Override
    public ResponseEntity<List<StaffAreaCoverageRule>> listStaffAreaCoverageRules() {
        return ResponseEntity.ok(staffAreaCoverageRuleService.list());
    }

    @Override
    public ResponseEntity<StaffAreaCoverageRule> setStaffAreaCoverageRule(StaffArea staffArea, StaffAreaCoverageRuleInput staffAreaCoverageRuleInput) {
        return ResponseEntity.ok(staffAreaCoverageRuleService.set(staffArea, staffAreaCoverageRuleInput, callerId()));
    }

    @Override
    public ResponseEntity<List<AttendancePunch>> listAttendancePunches(String employeeUserId, String from, String to) {
        return ResponseEntity.ok(attendanceService.list(employeeUserId, java.time.LocalDate.parse(from), java.time.LocalDate.parse(to)));
    }

    @Override
    public ResponseEntity<AttendancePunch> recordAttendancePunch(AttendancePunchCreateInput attendancePunchCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.recordPunch(attendancePunchCreateInput, callerId()));
    }

    @Override
    public ResponseEntity<List<AttendanceDaySummary>> getAttendanceSummary(String employeeUserId, Integer year, Integer month) {
        return ResponseEntity.ok(attendanceService.summary(employeeUserId, year, month));
    }

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
