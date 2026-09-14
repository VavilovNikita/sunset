package com.sunsetbeach.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SqlStatesTests {

    @Test
    void is_matchesTheSqlStateField_regardlessOfMessageText() {
        SQLException sqlException = new SQLException("deadlock detected", "40P01");
        RuntimeException wrapped = new RuntimeException("wrapper", sqlException);

        assertThat(SqlStates.is(wrapped, "40P01")).isTrue();
        assertThat(SqlStates.is(wrapped, "40001")).isFalse();
    }

    /** Regression guard for the exact bug this class exists to prevent: a match on the SQLSTATE field, never on text that happens to resemble one. */
    @Test
    void is_aMessageMentioningTheCodeInPassingDoesNotCount() {
        SQLException sqlException = new SQLException("some unrelated failure that happens to mention 40P01 in its own text", "58030");

        assertThat(SqlStates.is(sqlException, "40P01")).isFalse();
    }

    @Test
    void find_returnsTheMatchingSqlExceptionItself() {
        SQLException sqlException = new SQLException("deadlock detected", "40P01");
        RuntimeException wrapped = new RuntimeException("wrapper", sqlException);

        assertThat(SqlStates.find(wrapped, "40P01")).contains(sqlException);
    }

    @Test
    void find_walksMultipleLevelsOfWrapping() {
        SQLException sqlException = new SQLException("deadlock detected", "40P01");
        RuntimeException innerWrap = new RuntimeException("inner", sqlException);
        RuntimeException outerWrap = new RuntimeException("outer", innerWrap);

        assertThat(SqlStates.find(outerWrap, "40P01")).contains(sqlException);
    }

    @Test
    void find_noMatchAnywhereInTheChain_isEmpty() {
        RuntimeException noSqlExceptionAtAll = new RuntimeException("plain failure, no cause");

        assertThat(SqlStates.find(noSqlExceptionAtAll, "40P01")).isEmpty();
    }
}
