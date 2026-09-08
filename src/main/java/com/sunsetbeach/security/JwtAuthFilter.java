package com.sunsetbeach.security;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
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
                .ifPresent(parsed -> currentValidUser(parsed).ifPresent(user -> {
                    var authorities = new ArrayList<GrantedAuthority>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + parsed.principal().role().getValue()));
                    // Job functions are a second, independent authorization axis (see JobFunction) -
                    // granted from this fresh row, not from the token's own claims the way ROLE_
                    // above is. Role needs the tokenVersion-bump-and-re-login dance on a change
                    // (see UserService#updateRole) specifically because it's read from the token;
                    // functions never need that, since this DB read already happens every request.
                    for (String function : user.getJobFunctions()) {
                        authorities.add(new SimpleGrantedAuthority("FUNCTION_" + function));
                    }
                    var authentication = new UsernamePasswordAuthenticationToken(parsed.principal(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }));
        chain.doFilter(request, response);
    }

    /**
     * A signature-valid, unexpired token is not enough on its own: since JWTs can't be revoked
     * in place, every request re-checks the issuing user's *current* row - a disabled account or
     * a tokenVersion bumped since this token was issued (password change, role change, admin
     * reset, disable/enable) both fail here even though the token itself is still technically
     * valid. This is the one per-request DB round trip that makes revocation possible at all;
     * deliberately not cached, since the whole point is that a disable/reset must take effect on
     * the very next request, not after some TTL. Returns the entity (not just a boolean) so
     * doFilterInternal can also read its current job functions off the same row.
     */
    private Optional<UserEntity> currentValidUser(JwtService.ParsedToken parsed) {
        return userRepository
                .findById(parsed.principal().id())
                .filter(UserEntity::isActive)
                .filter(user -> user.getTokenVersion() == parsed.tokenVersion());
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
