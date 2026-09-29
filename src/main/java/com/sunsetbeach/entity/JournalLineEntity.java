package com.sunsetbeach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;

/** One side of a {@link JournalEntryEntity} - exactly one of debit/credit is non-zero (V120's CHECK). */
@Entity
@Immutable
@Table(name = "JournalLine")
public class JournalLineEntity {

    @Id
    @UuidGenerator
    private String id;

    private String entryId;

    private int lineOrder;

    private String accountCode;

    @Column(precision = 12, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    public String getId() {
        return id;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public void setLineOrder(int lineOrder) {
        this.lineOrder = lineOrder;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }
}
