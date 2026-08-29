package com.sunsetbeach.security;

import com.sunsetbeach.model.Role;
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
 * Issues and verifies the JWTs returned by {@code POST /auth/login}, replacing the old
 * NextAuth.js JWE session cookie as the sole authentication mechanism for this API.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.jwt-ttl-days}") long ttlDays) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDays);
    }

    /** Convenience overload for callers that don't track tokenVersion (e.g. tests) - issues a token as if tokenVersion were 0. */
    public String issue(StaffPrincipal principal) {
        return issue(principal, 0);
    }

    /**
     * {@code tokenVersion} must match the issuing user's current {@code User.tokenVersion} at
     * verification time (see {@link JwtAuthFilter}) - it's how an otherwise-stateless,
     * not-yet-expired JWT gets revoked: a password change, role change, admin reset, or
     * disable/enable all bump the stored value, which immediately invalidates every token
     * issued before that point.
     */
    public String issue(StaffPrincipal principal, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.id())
                .claim("email", principal.email())
                .claim("role", principal.role().getValue())
                .claim("tokenVersion", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** A verified token's claims, split into the identity ({@link StaffPrincipal}) and the revocation check ({@code tokenVersion}). */
    public record ParsedToken(StaffPrincipal principal, int tokenVersion) {
    }

    public Optional<ParsedToken> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

            String id = claims.getSubject();
            String email = claims.get("email", String.class);
            String roleValue = claims.get("role", String.class);
            if (id == null || roleValue == null) {
                return Optional.empty();
            }

            Role role;
            try {
                role = Role.fromValue(roleValue);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }

            // Absent only for a token issued before tokenVersion existed - treated as version 0,
            // which is also every current user's starting value, so such a token still validates
            // normally against a never-bumped account.
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);
            return Optional.of(new ParsedToken(new StaffPrincipal(id, email, role), tokenVersion != null ? tokenVersion : 0));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
