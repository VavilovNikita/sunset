package com.sunsetbeach.controller;

import com.sunsetbeach.api.RosterApi;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.EmployeePattern;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.EmployeePayRate;
import com.sunsetbeach.model.EmployeePayRateCreateInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.RosterEntry;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.RosterImportColorMappingInput;
import com.sunsetbeach.model.RosterImportColorMappingResult;
import com.sunsetbeach.model.RosterImportCommitInput;
import com.sunsetbeach.model.RosterImportNameMappingInput;
import com.sunsetbeach.model.RosterImportNameMappingResult;
import com.sunsetbeach.model.RosterImportPreview;
import com.sunsetbeach.model.RosterImportResult;
import com.sunsetbeach.model.RosterMonth;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.RosterReassignInput;
import com.sunsetbeach.model.RosterSwapInput;
import com.sunsetbeach.model.RosterEmployee;
import com.sunsetbeach.model.RosterLockInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeDisplayColorUpdateInput;
import com.sunsetbeach.model.ShiftCodeKindUpdateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.StaffAreaCoverageRule;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.AttendanceService;
import com.sunsetbeach.service.EmployeePatternService;
import com.sunsetbeach.service.EmployeePayRateService;
import com.sunsetbeach.service.RosterExportService;
import com.sunsetbeach.service.RosterImportService;
import com.sunsetbeach.service.RosterService;
import com.sunsetbeach.service.ShiftCodeService;
import com.sunsetbeach.service.StaffAreaCoverageRuleService;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RosterController implements RosterApi {

    private final ShiftCodeService shiftCodeService;
    private final EmployeePatternService employeePatternService;
    private final RosterService rosterService;
    private final RosterExportService rosterExportService;
    private final StaffAreaCoverageRuleService staffAreaCoverageRuleService;
    private final AttendanceService attendanceService;
    private final EmployeePayRateService employeePayRateService;
    private final RosterImportService rosterImportService;

    public RosterController(
            ShiftCodeService shiftCodeService,
            EmployeePatternService employeePatternService,
            RosterService rosterService,
            RosterExportService rosterExportService,
            StaffAreaCoverageRuleService staffAreaCoverageRuleService,
            AttendanceService attendanceService,
            EmployeePayRateService employeePayRateService,
            RosterImportService rosterImportService) {
        this.shiftCodeService = shiftCodeService;
        this.employeePatternService = employeePatternService;
        this.rosterService = rosterService;
        this.rosterExportService = rosterExportService;
        this.staffAreaCoverageRuleService = staffAreaCoverageRuleService;
        this.attendanceService = attendanceService;
        this.employeePayRateService = employeePayRateService;
        this.rosterImportService = rosterImportService;
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
    public ResponseEntity<ShiftCode> updateShiftCodeKind(String id, ShiftCodeKindUpdateInput shiftCodeKindUpdateInput) {
        return ResponseEntity.ok(shiftCodeService.updateKind(id, shiftCodeKindUpdateInput.getKind(), callerId()));
    }

    @Override
    public ResponseEntity<ShiftCode> updateShiftCodeDisplayColor(String id, ShiftCodeDisplayColorUpdateInput shiftCodeDisplayColorUpdateInput) {
        return ResponseEntity.ok(shiftCodeService.updateDisplayColor(id, shiftCodeDisplayColorUpdateInput.getDisplayColor().orElse(null), callerId()));
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

    @Override
    public ResponseEntity<List<TodayShiftStatus>> getTodayShiftBoard() {
        return ResponseEntity.ok(attendanceService.getTodayShiftBoard());
    }

    @Override
    public ResponseEntity<List<EmployeePayRate>> listEmployeePayRates(String employeeUserId) {
        return ResponseEntity.ok(employeePayRateService.list(employeeUserId));
    }

    @Override
    public ResponseEntity<EmployeePayRate> createEmployeePayRate(EmployeePayRateCreateInput employeePayRateCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeePayRateService.create(employeePayRateCreateInput, callerId()));
    }

    @Override
    public ResponseEntity<Resource> exportRosterActuals(Integer year, Integer month) {
        byte[] workbook = rosterExportService.exportActuals(year, month, callerId());
        Resource resource = new ByteArrayResource(workbook);
        String filename = "roster-actuals-" + year + "-" + String.format("%02d", month) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    @Override
    public ResponseEntity<Resource> exportRosterGrid(Integer year, Integer month) {
        byte[] workbook = rosterExportService.exportGrid(year, month, callerId());
        Resource resource = new ByteArrayResource(workbook);
        String filename = "roster-" + year + "-" + String.format("%02d", month) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    @Override
    public ResponseEntity<RosterImportPreview> previewRosterImport(MultipartFile file, Integer year, Integer month) {
        return ResponseEntity.ok(rosterImportService.preview(file, year, month));
    }

    @Override
    public ResponseEntity<RosterImportNameMappingResult> createRosterImportNameMapping(RosterImportNameMappingInput rosterImportNameMappingInput) {
        return ResponseEntity.ok(rosterImportService.createNameMapping(rosterImportNameMappingInput, callerId()));
    }

    @Override
    public ResponseEntity<RosterImportColorMappingResult> createRosterImportColorMapping(RosterImportColorMappingInput rosterImportColorMappingInput) {
        return ResponseEntity.ok(rosterImportService.createColorMapping(rosterImportColorMappingInput, callerId()));
    }

    @Override
    public ResponseEntity<RosterImportResult> commitRosterImport(RosterImportCommitInput rosterImportCommitInput) {
        return ResponseEntity.ok(rosterImportService.commit(rosterImportCommitInput.getImportId(), callerId()));
    }

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
