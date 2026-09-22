package com.sunsetbeach.security;

/** The guest-account analogue of {@link StaffPrincipal} - deliberately its own type, never mixed with it. */
public record GuestPrincipal(String id, String email) {
}
