package com.sunsetbeach.tuya;

import java.time.Clock;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Counts calls to Tuya per calendar month in hotel time (the Trial Edition stops at its quota, so
 * an ADMIN needs to see the spend). Like the audit log it writes in its own transaction and never
 * throws: a counter that fails must not stop a fan from switching.
 */
@Component
public class TuyaUsageCounter {

    private static final Logger log = LoggerFactory.getLogger(TuyaUsageCounter.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public TuyaUsageCounter(JdbcTemplate jdbc, PlatformTransactionManager txManager, Clock clock) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    public String currentMonth() {
        return YearMonth.now(clock).toString();
    }

    /** One call was made (counted whether Tuya accepted it or not - a rejected call still costs quota). */
    public void increment() {
        try {
            tx.executeWithoutResult(status -> jdbc.update(
                    "INSERT INTO \"TuyaApiUsage\" (month, calls) VALUES (?, 1) ON CONFLICT (month) DO UPDATE SET calls = \"TuyaApiUsage\".calls + 1",
                    currentMonth()));
        } catch (Exception e) {
            log.error("Failed to count a Tuya call", e);
        }
    }

    public long callsThisMonth() {
        Long calls = jdbc.query(
                "SELECT calls FROM \"TuyaApiUsage\" WHERE month = ?", rs -> rs.next() ? rs.getLong(1) : null, currentMonth());
        return calls == null ? 0 : calls;
    }
}
