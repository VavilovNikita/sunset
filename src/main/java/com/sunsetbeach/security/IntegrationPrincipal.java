package com.sunsetbeach.security;

/**
 * The authenticated caller behind {@link IntegrationKeyAuthFilter} - a machine, not a staff
 * member. Deliberately not a {@link StaffPrincipal} with a sentinel role: {@code Role} drives the
 * staff hierarchy, and anything that casts the principal to {@code StaffPrincipal} (the handful of
 * staff-only actions that record who did them) must fail rather than attribute a machine's call
 * to a made-up person. {@code AuditLogService} records it with the fixed actor {@link #auditActorId}.
 */
public record IntegrationPrincipal(String auditActorId, String auditActorEmail) {

    public static final IntegrationPrincipal SITEMINDER = new IntegrationPrincipal("SITEMINDER", "siteminder@sunsetbeach.internal");

    /** Granted instead of any {@code ROLE_}: gate a route on it with {@code hasAuthority}, never {@code hasRole}. */
    public static final String SITEMINDER_AUTHORITY = "INTEGRATION_SITEMINDER";
}
