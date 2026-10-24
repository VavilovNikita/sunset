package com.sunsetbeach.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared by {@link com.sunsetbeach.controller.AuthController}'s login rate limiting and the other
 * per-address limiters - they all key their buckets off the caller's address the same way.
 *
 * <p>Reads the <em>last</em> entry of {@code X-Forwarded-For}: the one written by the proxy
 * directly in front of this app, never by the caller. Two things sit in front of it: the host
 * nginx on {@code /api/}, whose {@code $proxy_add_x_forwarded_for} <em>appends</em> the address it
 * saw to whatever the caller sent, and the Next.js server, which sends a single value (the
 * visitor's {@code X-Real-IP}). The first entry used to be read - with the appending nginx that
 * is the caller's own text, so rotating it gave a password-guessing run a fresh bucket per
 * request (audit finding H2). The last entry can be the Cloudflare edge or tunnel instead of the
 * visitor, which puts more people in one bucket; the login limiter's key includes the email, so
 * that costs at most a lockout of one email from one edge, never a bypass.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            String last = hops[hops.length - 1].trim();
            if (!last.isEmpty()) {
                return last;
            }
        }
        return request.getRemoteAddr();
    }
}
