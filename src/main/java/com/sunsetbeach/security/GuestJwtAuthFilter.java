package com.sunsetbeach.security;

import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.repository.GuestAccountRepository;
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

/**
 * The guest-account analogue of {@link JwtAuthFilter} - parses a {@code guestBearerAuth} token
 * (see {@link GuestJwtService}, signed with its own secret) and grants a single {@code
 * ROLE_GUEST} authority. Registered alongside {@link JwtAuthFilter} at the same filter-chain
 * position (see SecurityConfig) - since the two schemes use different keys, presenting a staff
 * token never satisfies this filter and a guest token never satisfies the other, so both simply
 * run unconditionally on every request with no need to branch on which one a request "is for".
 */
public class GuestJwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GuestJwtService guestJwtService;
    private final GuestAccountRepository guestAccountRepository;

    public GuestJwtAuthFilter(GuestJwtService guestJwtService, GuestAccountRepository guestAccountRepository) {
        this.guestJwtService = guestJwtService;
        this.guestAccountRepository = guestAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        bearerToken(request)
                .flatMap(guestJwtService::parse)
                .ifPresent(parsed -> currentValidAccount(parsed).ifPresent(account -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            parsed.principal(), null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }));
        chain.doFilter(request, response);
    }

    /**
     * Same re-check-the-current-row pattern as {@link JwtAuthFilter#currentValidUser} - a
     * signature-valid, unexpired token still fails here if the account's {@code tokenVersion} has
     * since been bumped (a password change) or {@code emailVerifiedAt} has somehow gone back to
     * null (it never does today, but this keeps the invariant "unverified accounts never
     * authenticate" true regardless).
     */
    private Optional<GuestAccountEntity> currentValidAccount(GuestJwtService.ParsedToken parsed) {
        return guestAccountRepository
                .findById(parsed.principal().id())
                .filter(account -> account.getEmailVerifiedAt() != null)
                .filter(account -> account.getTokenVersion() == parsed.tokenVersion());
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
