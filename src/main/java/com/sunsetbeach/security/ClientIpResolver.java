package com.sunsetbeach.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared by {@link com.sunsetbeach.controller.AuthController}'s login rate limiting and
 * {@link BookingRateLimiter} - both key their limiter buckets off the caller's IP the same way.
 * Trusts {@code X-Forwarded-For}'s first hop, which only reflects the real client address when
 * this app sits behind a reverse proxy that overwrites (rather than appends to) that header for
 * inbound requests; with no such proxy in front of it, the header is client-supplied and this
 * resolves to whatever the caller claims.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
