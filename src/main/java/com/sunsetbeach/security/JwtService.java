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

    public String issue(StaffPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.id())
                .claim("email", principal.email())
                .claim("role", principal.role().getValue())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Optional<StaffPrincipal> parse(String token) {
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

            return Optional.of(new StaffPrincipal(id, email, role));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
