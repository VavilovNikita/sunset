package com.sunsetbeach.security;

import com.sunsetbeach.model.Role;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the {@link StaffPrincipal} the JWT filter put in the security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String id() {
        return principal().id();
    }

    public static Role role() {
        return principal().role();
    }

    private static StaffPrincipal principal() {
        return (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
