package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * A guest's own persistent, self-service login - see this table's own V89 migration comment and
 * GuestAccount's openapi.yaml description for why this is a brand-new identity system, entirely
 * separate from "User" (staff) and "Guest" (the CRM contact card).
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

    // Null means unverified/unusable - see GuestJwtAuthFilter, which rejects every token for an
    // account whose row still has this null, the same way JwtAuthFilter rejects a disabled User.
    private LocalDateTime emailVerifiedAt;

    // Nullable together, cleared on successful verify - only one pending token makes sense at a
    // time, so a fresh register/resend simply overwrites both rather than needing a token table.
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiresAt;

    // Bumped on a self-service password change - see GuestJwtAuthFilter, which rejects any token
    // whose tokenVersion claim doesn't match the current value here. Same mechanism as
    // User.tokenVersion.
    private int tokenVersion = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
