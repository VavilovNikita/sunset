package com.sunsetbeach.security;

import com.sunsetbeach.model.Role;

public record StaffPrincipal(String id, String email, Role role) {
}
