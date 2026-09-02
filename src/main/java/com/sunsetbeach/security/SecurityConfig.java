package com.sunsetbeach.security;

import com.sunsetbeach.repository.UserRepository;
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
            UserRepository userRepository,
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
                        // Self-service password change: any authenticated staff role, no ADMIN
                        // needed - this is the one account-security action every user can take
                        // on their own account without help. See PATCH /users/{id}/password for
                        // the ADMIN-only counterpart that resets *someone else's* password.
                        .requestMatchers(HttpMethod.PATCH, "/auth/password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole(com.sunsetbeach.model.Role.ADMIN.getValue())
                        // Hard-restricted to ADMIN regardless of RoleHierarchy - MANAGER/CASHIER/WAITER
                        // never get here even though the hierarchy grants them everything below ADMIN
                        // elsewhere (see PosRoleHierarchyTests for the regression test).
                        .requestMatchers("/users/**").hasRole(com.sunsetbeach.model.Role.ADMIN.getValue())
                        // Room *type* management (Rooms/Pricing tags): reads are CASHIER+ - a CASHIER
                        // creating a walk-in booking via POST /bookings/staff needs to be able to name
                        // the room type and quote its price through an authenticated endpoint, rather
                        // than being forced to the public, unauthenticated GET /public/rooms/** to do
                        // their own job (the same "action allowed, prerequisite read blocked" asymmetry
                        // already fixed for RoomUnits/Availability below). Writes (create/update/delete
                        // a room type, upload/delete images, set price overrides) stay MANAGER-only.
                        // GET matchers must come first - more specific (method + path) than the general
                        // rule below, same ordering requirement as /room-units.
                        .requestMatchers(HttpMethod.GET, "/rooms", "/rooms/*").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers("/rooms/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.GET, "/pricing/*").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers("/pricing/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Physical room CRUD (RoomUnit): reads are WAITER+ (room numbers aren't
                        // sensitive - and a CASHIER assigning a room to a booking via PUT
                        // /bookings/{id}/room-unit below needs to be able to list candidates in the
                        // first place, or the action is permitted but unusable). Writes stay
                        // MANAGER-only. The GET matcher must come first - more specific
                        // (method + path) than the general rule below, same ordering requirement as
                        // /orders/*/close vs /orders/** further down.
                        //
                        // The manual-block sub-resource (/room-units/{id}/blocks/**) stays
                        // MANAGER+ for BOTH reads and writes, deliberately not loosened alongside the
                        // parent resource: a block's `reason` is free text staff write for each other
                        // ("owner's dog chewed the carpet, do not sell until replaced") and can carry
                        // internal notes not meant for every role - frontend should treat this list as
                        // MANAGER-only, not assume it follows GET /room-units's visibility.
                        .requestMatchers(HttpMethod.GET, "/room-units", "/room-units/*").hasRole(com.sunsetbeach.model.Role.WAITER.getValue())
                        // Housekeeping status is a deliberately lower bar than the rest of
                        // /room-units/** (MANAGER+ below) - front desk flips this day to day, not
                        // just managers - so it needs its own rule ahead of the catch-all, same
                        // ordering requirement as the GET carve-out above.
                        .requestMatchers(HttpMethod.PATCH, "/room-units/*/housekeeping").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // PATCH /room-units/positions (the property map editor's batch save) needs no
                        // separate rule here - it isn't the GET or housekeeping carve-out above, so it
                        // falls straight through to the MANAGER+ catch-all below, which is exactly the
                        // role this write needs. Same "no separate rule needed" reasoning as
                        // GET /bookings/today further down.
                        .requestMatchers("/room-units/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Property map: viewing (both the aggregate JSON and the background image
                        // itself) is CASHIER+, same floor as GET /bookings/today - this is the front
                        // desk's own screen, not a manager report. Replacing the background image is
                        // MANAGER+, matching the room-photo upload precedent (POST /rooms/{id}/images).
                        // Deliberately NOT under /uploads/** (permitAll, public-site room photos) -
                        // this image is an internal tool, never reachable from the public site.
                        .requestMatchers(HttpMethod.GET, "/property-map", "/property-map/image").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/property-map/image").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Availability: CASHIER+, not MANAGER+ as openapi.yaml originally documented -
                        // this description predates front-desk room assignment. The per-unit breakdown
                        // here is exactly what a CASHIER needs to see which physical room is free
                        // before calling PUT /bookings/{id}/room-unit (also CASHIER+, below) - gating
                        // it at MANAGER+ would recreate the same "action allowed, prerequisite read
                        // blocked" asymmetry that GET /room-units above was loosened to fix.
                        .requestMatchers(HttpMethod.GET, "/availability/*").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Bookings: same reasoning as availability above - openapi.yaml documented
                        // list/get/status-update as ADMIN/MANAGER, written before this app had a
                        // front-desk role at all. Finding a guest's booking, changing its status/
                        // payment note, and seeing per-unit availability to assign a room are the
                        // front desk's actual job, so all four are CASHIER+ (openapi.yaml updated to
                        // match, not left stale). PUT .../room-unit is already CASHIER+ above.
                        // GET /bookings/export is the one deliberate exception: bulk CSV of every
                        // guest's name/email/phone across a date range is a different risk profile
                        // than looking up one booking at a time, so it stays MANAGER+ - and its rule
                        // must come first, since "/bookings/*" below would otherwise also match the
                        // literal path segment "export".
                        .requestMatchers(HttpMethod.GET, "/bookings/*/folio", "/bookings/*/pos-orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/bookings/export").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // GET /bookings/calendar is matched by the general GET /bookings/* rule
                        // below (same CASHIER+ role, no reason to loosen/tighten it separately) -
                        // Ant-style "*" matches exactly one path segment, same as how
                        // GET /bookings/export above already coexists with it. Left unlisted here
                        // deliberately; EndpointCoverageTests confirms it isn't falling through to
                        // the anyRequest() catch-all.
                        .requestMatchers(HttpMethod.GET, "/bookings", "/bookings/*").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/bookings/*").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Assigning/unassigning a booking's physical room is a front-desk (CASHIER+)
                        // operation - explicitly matched (rather than left to a general /bookings/**
                        // rule) since it has its own role independent of the PATCH above.
                        .requestMatchers(HttpMethod.PUT, "/bookings/*/room-unit").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Front-desk booking creation (POST /bookings/staff) and the booking
                        // calendar grid's schedule-change operations - all CASHIER+, all
                        // two-segment-plus paths past "/bookings/" so none of them are covered by
                        // the single-segment "/bookings/*" wildcard above and each needs its own
                        // explicit rule (same reasoning as PUT /bookings/*/room-unit).
                        .requestMatchers(HttpMethod.POST, "/bookings/staff").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/schedule/quote").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/bookings/*/schedule").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Mid-stay room relocation (a guest moving to a different room, possibly a
                        // different room type) - same CASHIER+ tier as the rest of front-desk
                        // reservation work above, same explicit-rule requirement (multi-segment paths
                        // past "/bookings/" aren't covered by "/bookings/*"). The quote path has one
                        // extra segment ("relocate/quote") than "relocate"/"undo-relocation" - Ant "*"
                        // matches exactly one segment, so it needs its own matcher, not a shared one.
                        .requestMatchers(HttpMethod.POST, "/bookings/*/relocate/quote").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/relocate").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/undo-relocation").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Repricing a segment to current rates is a deliberate override of an
                        // already-agreed price, not front-desk administration of a guest's own
                        // request - one tier above the CASHIER-level schedule/relocate operations
                        // above, same explicit-rule requirement (multi-segment paths past
                        // "/bookings/" aren't covered by "/bookings/*").
                        .requestMatchers(HttpMethod.POST, "/bookings/*/reprice/quote").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/reprice").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Physical occupancy (check-in/check-out/no-show) - same CASHIER+ tier as
                        // the rest of front-desk reservation work above, same explicit-rule
                        // requirement (two-segment paths past "/bookings/" aren't covered by
                        // "/bookings/*"). GET /bookings/today needs no separate rule - it's a
                        // single path segment, already covered by the general
                        // "/bookings", "/bookings/*" GET rule above at the same CASHIER+ role.
                        .requestMatchers(HttpMethod.POST, "/bookings/*/check-in").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/check-out").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.POST, "/bookings/*/no-show").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // Folio payments: recording one is money-handling, CASHIER+ same as the
                        // rest of front-desk work above. Listing them is informational (the same
                        // "what's owed" story GET /bookings/*/folio already tells any
                        // authenticated role) so it stays open, matching that endpoint's own bar
                        // rather than the write's.
                        .requestMatchers(HttpMethod.GET, "/bookings/*/folio-payments").authenticated()
                        .requestMatchers(HttpMethod.POST, "/bookings/*/folio-payments").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        // POS module: read endpoints (GET /menu, /tables, /orders/**) are open to any
                        // authenticated staff role, including WAITER - explicitly matched below rather
                        // than left to fall through to anyRequest(), so the EndpointCoverageTests
                        // guard (every openapi.yaml path must be claimed by a specific rule before the
                        // final anyRequest() catch-all) passes for them too. Mutating endpoints are
                        // gated per the agreed contract:
                        .requestMatchers(HttpMethod.POST, "/menu").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/menu/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.DELETE, "/menu/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.GET, "/menu", "/menu/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/tables").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.PATCH, "/tables/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.GET, "/tables").authenticated()
                        // More specific than the /orders/** rule below, so it must come first -
                        // authorizeHttpRequests matches in declaration order.
                        .requestMatchers(HttpMethod.POST, "/orders/*/close").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers("/orders/**").hasRole(com.sunsetbeach.model.Role.WAITER.getValue())
                        // Same ordering requirement: /shifts/*/export and the bare /shifts list
                        // (till reconciliation across many shifts, same tier as the export and
                        // the audit log below) before the general /shifts/** rule.
                        .requestMatchers(HttpMethod.GET, "/shifts/*/export").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers(HttpMethod.GET, "/shifts").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers("/shifts/**").hasRole(com.sunsetbeach.model.Role.CASHIER.getValue())
                        .requestMatchers(HttpMethod.GET, "/payments/summary").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Audit log: read-only, MANAGER+ - the disputes it exists to resolve (a
                        // cash discrepancy, a guest billing question, a suspected misuse of a
                        // role) are exactly what a manager needs to investigate without
                        // escalating to an admin, same reasoning as /payments/summary above and
                        // the booking/shift CSV exports. There is no write route for any role -
                        // every entry is written internally by AuditLogService, never through
                        // this API.
                        .requestMatchers(HttpMethod.GET, "/audit-log").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        // Printer CRUD/test (registering, editing, deleting, test-printing a physical
                        // printer) is MANAGER-only. The print-job queue is WAITER+ - any staff role may
                        // view/retry it, but PrinterService itself filters what a non-MANAGER caller can
                        // see/retry down to KITCHEN_TICKET/BAR_TICKET/PREBILL (Z-reports and guest receipts are
                        // cashier/management information - see PrinterService#isVisible).
                        .requestMatchers("/printers/**").hasRole(com.sunsetbeach.model.Role.MANAGER.getValue())
                        .requestMatchers("/print-jobs/**").hasRole(com.sunsetbeach.model.Role.WAITER.getValue())
                        // GET /auth/me is the one endpoint outside all the tag groups above - any
                        // valid JWT, no role check (it just echoes back who the token belongs to).
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        // Deliberately left empty: every openapi.yaml path above this point is now
                        // claimed by a specific rule. anyRequest().authenticated() stays as a safety
                        // net for anything not yet in the contract, not as a place new endpoint groups
                        // silently land in - see EndpointCoverageTests, which fails the build the next
                        // time that happens instead of relying on someone noticing in review.
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthFilter(jwtService, userRepository), UsernamePasswordAuthenticationFilter.class);
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
