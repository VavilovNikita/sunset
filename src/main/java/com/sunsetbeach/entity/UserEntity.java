package com.sunsetbeach.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.sunsetbeach.model.Role;

@Entity
@Table(name = "User")
public class UserEntity {

    @Id
    @UuidGenerator
    private String id;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Role role = Role.MANAGER;

    // Whether this account can currently authenticate - see JwtAuthFilter, which rejects every
    // request bearing a disabled user's token regardless of the token's own expiry. Named
    // isActive (not active), matching RoomUnitEntity's convention, since the DB column is
    // "isActive" and this project's naming strategy maps a field to a column of the same name.
    private boolean isActive = true;

    // Bumped on password change, role change, an admin password reset, or a disable/enable -
    // see JwtAuthFilter, which rejects any token whose tokenVersion claim doesn't match the
    // current value here. This is the only way a stateless JWT gets revoked before it expires.
    private int tokenVersion = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }
}
