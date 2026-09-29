package com.sunsetbeach.entity;

import com.sunsetbeach.model.LedgerAccountType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One account in the chart - see V120's own comment. Keyed by its code; never renamed, retyped
 * or deleted, hence {@link Immutable}.
 */
@Entity
@Immutable
@Table(name = "LedgerAccount")
public class LedgerAccountEntity {

    @Id
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LedgerAccountType type;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LedgerAccountType getType() {
        return type;
    }

    public void setType(LedgerAccountType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
