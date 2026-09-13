package com.sunsetbeach.controller;

import com.sunsetbeach.api.RosterApi;
import com.sunsetbeach.model.EmployeePattern;
import com.sunsetbeach.model.EmployeePatternInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.EmployeePatternService;
import com.sunsetbeach.service.ShiftCodeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the shift-code and employee-pattern slice of the Roster module first - the rest of
 * {@link RosterApi} (the roster grid itself, coverage, attendance, pay rates, export) lands in
 * later commits, each adding its own {@code @Override}s and constructor dependency here. Until
 * then, RosterApi's own default methods answer those paths with 501, same as any endpoint not
 * yet implemented.
 */
@RestController
public class RosterController implements RosterApi {

    private final ShiftCodeService shiftCodeService;
    private final EmployeePatternService employeePatternService;

    public RosterController(ShiftCodeService shiftCodeService, EmployeePatternService employeePatternService) {
        this.shiftCodeService = shiftCodeService;
        this.employeePatternService = employeePatternService;
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

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
