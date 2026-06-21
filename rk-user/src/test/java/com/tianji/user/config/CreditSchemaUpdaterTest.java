package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureCreditSchema_shouldCreateTableAndSeedDefaultsWhenMissing() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'credit_type'"), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'user_credit_record'"), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'user_credit_summary'"), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("FROM credit_type"), eq(Integer.class))).thenReturn(0);

        CreditSchemaUpdater updater = new CreditSchemaUpdater(jdbcTemplate);

        updater.ensureCreditSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE credit_type"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE user_credit_record"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE user_credit_summary"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("INSERT INTO credit_type"));
    }

    @Test
    void ensureCreditSchema_shouldBackfillMissingAuditColumnsForExistingTables() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'credit_type'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'user_credit_record'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'user_credit_summary'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("FROM credit_type"), eq(Integer.class))).thenReturn(1);

        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'credit_type' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("creator")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'credit_type' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("updater")
        )).thenReturn(0);

        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_record' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("creator")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_record' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("updater")
        )).thenReturn(0);

        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_summary' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("create_time")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_summary' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("update_time")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_summary' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("creator")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'user_credit_summary' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("updater")
        )).thenReturn(0);

        CreditSchemaUpdater updater = new CreditSchemaUpdater(jdbcTemplate);

        updater.ensureCreditSchema();

        verify(jdbcTemplate).execute(contains("ALTER TABLE credit_type ADD COLUMN creator"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE credit_type ADD COLUMN updater"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_record ADD COLUMN creator"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_record ADD COLUMN updater"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_summary ADD COLUMN create_time"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_summary ADD COLUMN update_time"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_summary ADD COLUMN creator"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE user_credit_summary ADD COLUMN updater"));
        verify(jdbcTemplate, never()).execute(contains("INSERT INTO credit_type"));
    }
}
