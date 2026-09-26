package com.sunsetbeach.controller;

import com.sunsetbeach.api.UsersApi;
import com.sunsetbeach.model.ResetPasswordInput;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserActiveUpdateInput;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserCredentialsInput;
import com.sunsetbeach.model.UserEnrollmentNumberUpdateInput;
import com.sunsetbeach.model.UserFullNameUpdateInput;
import com.sunsetbeach.model.UserFunctionsUpdateInput;
import com.sunsetbeach.model.UserNameUpdateInput;
import com.sunsetbeach.model.UserOvertimeEligibilityUpdateInput;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.model.UserStaffAreaUpdateInput;
import com.sunsetbeach.model.UserUpdateResult;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UsersApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.list());
    }

    @Override
    public ResponseEntity<User> createUser(UserCreateInput userCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userCreateInput));
    }

    @Override
    public ResponseEntity<User> updateUserRole(String id, UserRoleUpdateInput userRoleUpdateInput) {
        String callerId = callerId();
        return ResponseEntity.ok(userService.updateRole(id, callerId, userRoleUpdateInput));
    }

    @Override
    public ResponseEntity<User> resetUserPassword(String id, ResetPasswordInput resetPasswordInput) {
        return ResponseEntity.ok(userService.resetPassword(id, resetPasswordInput.getNewPassword()));
    }

    @Override
    public ResponseEntity<User> grantUserCredentials(String id, UserCredentialsInput userCredentialsInput) {
        return ResponseEntity.ok(userService.grantCredentials(id, userCredentialsInput.getEmail(), userCredentialsInput.getPassword()));
    }

    @Override
    public ResponseEntity<UserUpdateResult> updateUserActive(String id, UserActiveUpdateInput userActiveUpdateInput) {
        String callerId = callerId();
        return ResponseEntity.ok(userService.setActive(id, callerId, userActiveUpdateInput.getActive()));
    }

    @Override
    public ResponseEntity<UserUpdateResult> updateUserFunctions(String id, UserFunctionsUpdateInput userFunctionsUpdateInput) {
        return ResponseEntity.ok(userService.updateFunctions(id, userFunctionsUpdateInput));
    }

    @Override
    public ResponseEntity<User> updateUserOvertimeEligibility(String id, UserOvertimeEligibilityUpdateInput userOvertimeEligibilityUpdateInput) {
        return ResponseEntity.ok(userService.updateOvertimeEligible(id, userOvertimeEligibilityUpdateInput.getOvertimeEligible()));
    }

    @Override
    public ResponseEntity<User> updateUserName(String id, UserNameUpdateInput userNameUpdateInput) {
        return ResponseEntity.ok(userService.updateName(id, userNameUpdateInput.getName()));
    }

    @Override
    public ResponseEntity<User> updateUserFullName(String id, UserFullNameUpdateInput userFullNameUpdateInput) {
        return ResponseEntity.ok(userService.updateFullName(id, userFullNameUpdateInput.getFullName()));
    }

    @Override
    public ResponseEntity<User> updateUserEnrollmentNumber(String id, UserEnrollmentNumberUpdateInput userEnrollmentNumberUpdateInput) {
        return ResponseEntity.ok(userService.updateEnrollmentNumber(id, userEnrollmentNumberUpdateInput.getEnrollmentNumber()));
    }

    @Override
    public ResponseEntity<User> updateUserStaffArea(String id, UserStaffAreaUpdateInput userStaffAreaUpdateInput) {
        return ResponseEntity.ok(userService.updateStaffArea(id, userStaffAreaUpdateInput.getStaffArea()));
    }

    private static String callerId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }
}
