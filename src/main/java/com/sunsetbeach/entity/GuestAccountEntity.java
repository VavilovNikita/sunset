package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.hibernate.annotations.UuidGenerator;

/**
 * A guest's own persistent, self-service login - see this table's own V89 migration comment and
 * GuestAccount's openapi.yaml description for why this is a brand-new identity system, entirely
 * separate from "User" (staff). It is linked to "Guest" (the CRM contact card) by {@code guestId}
 * since V104, but carries its own credentials - "Guest" never holds any.
 */
@Entity
@Table(name = "GuestAccount")
public class GuestAccountEntity {

    @Id
    @UuidGenerator
    private String id;

    // Always stored lowercased - GuestAccountService is the only writer. Unlike User.email (see
    // V68's own comment), there's no pre-existing mixed-case data to accommodate here, so a plain
    // unique index on the stored value is enough.
    private String email;

    private String passwordHash;

    private String name;

    // The CRM contact card this account belongs to - see V104's own comment and GuestLinkService.
    // Null when no single Guest could be matched by email (none yet, or an ambiguous duplicate).
    private String guestId;

    // Null means unverified/unusable - see GuestJwtAuthFilter, which rejects every token for an
    // account whose row still has this null, the same way JwtAuthFilter rejects a disabled User.
    private LocalDateTime emailVerifiedAt;

    // Nullable together, cleared on successful verify - only one pending token makes sense at a
    // time, so a fresh register/resend simply overwrites both rather than needing a token table.
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiresAt;

    // Nullable together (V130). Only the SHA-256 (hex) of the emailed reset token is stored, and
    // the expiry is hotel wall-clock from the shared Clock - see GuestAccountService#requestPasswordReset.
    private String passwordResetTokenHash;
    private LocalDateTime passwordResetExpiresAt;

    // Bumped on a self-service password change - see GuestJwtAuthFilter, which rejects any token
    // whose tokenVersion claim doesn't match the current value here. Same mechanism as
    // User.tokenVersion.
    private int tokenVersion = 0;

    // One switch for every automated lifecycle email (pre-arrival, post-stay, win-back) - see
    // LifecycleEmailService. Set by GET /guest-auth/unsubscribe, never cleared by anything.
    private boolean marketingEmailsOptOut = false;

    // The secret in every lifecycle email's unsubscribe link - see V110's own comment. Unlike
    // emailVerificationToken it is issued once and never rotated or cleared, so a link in an old
    // email keeps working. Filled in on first insert (see assignUnsubscribeToken), not by
    // GuestAccountService, so no creation path can leave it null against the NOT NULL column.
    private String unsubscribeToken;

    @UtcCreationTimestamp
    private LocalDateTime createdAt;

    @UtcUpdateTimestamp
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public LocalDateTime getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
    }

    public void setEmailVerificationExpiresAt(LocalDateTime emailVerificationExpiresAt) {
        this.emailVerificationExpiresAt = emailVerificationExpiresAt;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public boolean isMarketingEmailsOptOut() {
        return marketingEmailsOptOut;
    }

    public void setMarketingEmailsOptOut(boolean marketingEmailsOptOut) {
        this.marketingEmailsOptOut = marketingEmailsOptOut;
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    @PrePersist
    void assignUnsubscribeToken() {
        if (unsubscribeToken == null) {
            byte[] bytes = new byte[24];
            new SecureRandom().nextBytes(bytes);
            unsubscribeToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPasswordResetTokenHash() {
        return passwordResetTokenHash;
    }

    public void setPasswordResetTokenHash(String passwordResetTokenHash) {
        this.passwordResetTokenHash = passwordResetTokenHash;
    }

    public LocalDateTime getPasswordResetExpiresAt() {
        return passwordResetExpiresAt;
    }

    public void setPasswordResetExpiresAt(LocalDateTime passwordResetExpiresAt) {
        this.passwordResetExpiresAt = passwordResetExpiresAt;
    }
}
