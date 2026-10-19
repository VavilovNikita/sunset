package com.sunsetbeach.controller;

import com.sunsetbeach.api.ShiftRequestsApi;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.HrVacationInput;
import com.sunsetbeach.model.ShiftChangeDecisionInput;
import com.sunsetbeach.model.ShiftChangeRequest;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestStatus;
import com.sunsetbeach.model.ShiftRequestDeciders;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.ShiftChangeRequestService;
import com.sunsetbeach.service.ShiftRequestDeciderService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes: reading is MANAGER+ or the HR function, deciding and withdrawing are ADMIN or the HR function
 * ({@code SecurityConfig}); whether the caller is one of <em>today's</em> deciders is the service's call,
 * because it depends on leave dates and the fallback setting that a route can't see.
 */
@RestController
public class ShiftRequestsController implements ShiftRequestsApi {

    private final ShiftChangeRequestService requestService;
    private final ShiftRequestDeciderService deciderService;
    private final UserRepository userRepository;

    public ShiftRequestsController(ShiftChangeRequestService requestService, ShiftRequestDeciderService deciderService, UserRepository userRepository) {
        this.requestService = requestService;
        this.deciderService = deciderService;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<List<ShiftChangeRequest>> listShiftChangeRequests(ShiftChangeRequestStatus status, String from, String to, String employeeUserId) {
        return ResponseEntity.ok(requestService.list(status, date(from, "from"), date(to, "to"), employeeUserId, caller()));
    }

    @Override
    public ResponseEntity<ShiftRequestDeciders> getShiftRequestDeciders() {
        return ResponseEntity.ok(deciderService.view(caller()));
    }

    @Override
    public ResponseEntity<ShiftRequestDeciders> setHrVacation(HrVacationInput input) {
        return ResponseEntity.ok(deciderService.setVacation(caller(), date(input.getFrom(), "from"), date(input.getTo(), "to"), null));
    }

    @Override
    public ResponseEntity<ShiftChangeRequest> decideShiftChangeRequest(String id, ShiftChangeDecisionInput input) {
        UserEntity caller = caller();
        var result = requestService.decide(id, caller, input.getDecision() == ShiftChangeDecisionInput.DecisionEnum.APPROVE, input.getComment(), ShiftChangeRequestChannel.ADMIN_UI);
        return ResponseEntity.ok(requestService.toDto(result.request(), caller));
    }

    @Override
    public ResponseEntity<ShiftChangeRequest> cancelShiftChangeRequest(String id) {
        UserEntity caller = caller();
        var result = requestService.cancel(id, caller, ShiftChangeRequestChannel.ADMIN_UI);
        return ResponseEntity.ok(requestService.toDto(result.request(), caller));
    }

    private static LocalDate date(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ValidationException.field(field, "Must be a date like 2026-11-03");
        }
    }

    private UserEntity caller() {
        String id = ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
        return userRepository.findById(id).orElseThrow(() -> new BadRequestException("Unknown user"));
    }
}
