package com.sunsetbeach.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * All login/session management lives in this app - see AuthController for
 * POST /auth/login|register and GET /auth/me. Every authenticated request carries the JWT
 * issued by login in an {@code Authorization: Bearer} header (see JwtAuthFilter); there is no
 * server-side session state.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            RestAuthEntryPoint authEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/bookings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole(com.sunsetbeach.model.Role.ADMIN.getValue())
                        // Hard-restricted to ADMIN regardless of RoleHierarchy - MANAGER/CASHIER/WAITER
                        // never get here even though the hierarchy grants them everything below ADMIN
                        // elsewhere (see PosRoleHierarchyTests for the regression test).
                        .requestMatchers("/users/**").hasRole(com.sunsetbeach.model.Role.ADMIN.getValue())
                        // POS module: read endpoints (GET /menu, /tables, /orders/**) fall through to
                        // the anyRequest().authenticated() rule below - every staff role, including
                        // WAITER, may read them. Mutating endpoints are gated per the agreed contract:
                        .requestMatchers(HttpMethod.POST, "/menu").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/menu/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.DELETE, "/menu/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.POST, "/tables").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/tables/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // More specific than the /orders/** rule below, so it must come first -
                        // authorizeHttpRequests matches in declaration order.
                        .requestMatchers(HttpMethod.POST, "/orders/*/close").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers("/orders/**").hasRole(com.sunsetbeach.model.Role.WAITER.getValue())
                        // Same ordering requirement: /shifts/*/export before the general /shifts/** rule.
                        .requestMatchers(HttpMethod.GET, "/shifts/*/export").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers("/shifts/**").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.GET, "/payments/summary").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Printer CRUD/test (registering, editing, deleting, test-printing a physical
                        // printer) is MANAGER-only. The print-job queue is WAITER+ - any staff role may
                        // view/retry it, but PrinterService itself filters what a non-MANAGER caller can
                        // see/retry down to KITCHEN_TICKET/BAR_TICKET/PREBILL (Z-reports and guest receipts are
                        // cashier/management information - see PrinterService#isVisible).
                        .requestMatchers("/printers/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers("/print-jobs/**").hasRole(com.sunsetbeach.model.Role.WAITER.getValue())
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * ADMIN > MANAGER > CASHIER > WAITER. Spring Security's authorizeHttpRequests DSL picks up
     * a RoleHierarchy bean from the context automatically for every hasRole()/hasAnyRole() call
     * built through it (since 6.3 - see AuthorizeHttpRequestsConfigurer#authorizationManagerFactory),
     * so this is the only wiring needed - no explicit expression handler required. The one
     * exception is `/users/**`, matched with a literal hasRole(ADMIN) check above that this
     * hierarchy does not loosen (WAITER < CASHIER < MANAGER < ADMIN still means only ADMIN
     * satisfies it).
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_MANAGER > ROLE_CASHIER
                ROLE_CASHIER > ROLE_WAITER
                """);
    }
}
