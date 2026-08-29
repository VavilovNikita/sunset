package com.sunsetbeach.security;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        bearerToken(request)
                .flatMap(jwtService::parse)
                .filter(this::isCurrentlyValid)
                .ifPresent(parsed -> {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + parsed.principal().role().getValue()));
                    var authentication = new UsernamePasswordAuthenticationToken(parsed.principal(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        chain.doFilter(request, response);
    }

    /**
     * A signature-valid, unexpired token is not enough on its own: since JWTs can't be revoked
     * in place, every request re-checks the issuing user's *current* row - a disabled account or
     * a tokenVersion bumped since this token was issued (password change, role change, admin
     * reset, disable/enable) both fail here even though the token itself is still technically
     * valid. This is the one per-request DB round trip that makes revocation possible at all;
     * deliberately not cached, since the whole point is that a disable/reset must take effect on
     * the very next request, not after some TTL.
     */
    private boolean isCurrentlyValid(JwtService.ParsedToken parsed) {
        return userRepository
                .findById(parsed.principal().id())
                .filter(UserEntity::isActive)
                .filter(user -> user.getTokenVersion() == parsed.tokenVersion())
                .isPresent();
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
