package com.sunsetbeach.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the {@link GuestPrincipal} {@link GuestJwtAuthFilter} put in the security context - the guest-account analogue of {@link CurrentUser}. */
public final class CurrentGuest {

    private CurrentGuest() {
    }

    public static String id() {
        return principal().id();
    }

    public static String email() {
        return principal().email();
    }

    private static GuestPrincipal principal() {
        return (GuestPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
