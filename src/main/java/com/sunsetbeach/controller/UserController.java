package com.sunsetbeach.controller;

import com.sunsetbeach.api.UsersApi;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.UserService;
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
    public ResponseEntity<User> createUser(UserCreateInput userCreateInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userCreateInput));
    }

    @Override
    public ResponseEntity<User> updateUserRole(String id, UserRoleUpdateInput userRoleUpdateInput) {
        String callerId = ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
        return ResponseEntity.ok(userService.updateRole(id, callerId, userRoleUpdateInput));
    }
}
