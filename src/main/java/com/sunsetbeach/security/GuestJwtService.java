package com.sunsetbeach.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies guest-account JWTs - the same shape as {@link JwtService}, but signed with
 * its own secret ({@code app.security.guest-jwt-secret}, never {@code app.security.jwt-secret}).
 * This is the actual safety boundary between the two identity systems: a guest token can never
 * verify against the staff key or vice versa, so they can't bleed into each other even if a
 * filter is misconfigured later - see {@link GuestJwtAuthFilter} and {@link JwtAuthFilter}, which
 * run independently and unconditionally on every request precisely because of this.
 */
@Service
public class GuestJwtService {

    private final SecretKey key;
    private final Duration ttl;

    public GuestJwtService(
            @Value("${app.security.guest-jwt-secret}") String guestJwtSecret,
            @Value("${app.security.guest-jwt-ttl-days}") long ttlDays) {
        this.key = Keys.hmacShaKeyFor(guestJwtSecret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDays);
    }

    /**
     * {@code tokenVersion} must match the issuing account's current
     * {@code GuestAccountEntity.tokenVersion} at verification time (see
     * {@link GuestJwtAuthFilter}) - the same revocation mechanism {@link JwtService#issue} uses
     * for staff: a password change bumps the stored value, immediately invalidating every token
     * issued before that point.
     */
    public String issue(GuestPrincipal principal, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.id())
                .claim("email", principal.email())
                .claim("tokenVersion", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** A verified token's claims, split into the identity ({@link GuestPrincipal}) and the revocation check ({@code tokenVersion}). */
    public record ParsedToken(GuestPrincipal principal, int tokenVersion) {
    }

    public Optional<ParsedToken> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

            String id = claims.getSubject();
            String email = claims.get("email", String.class);
            if (id == null || email == null) {
                return Optional.empty();
            }

            Integer tokenVersion = claims.get("tokenVersion", Integer.class);
            return Optional.of(new ParsedToken(new GuestPrincipal(id, email), tokenVersion != null ? tokenVersion : 0));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
