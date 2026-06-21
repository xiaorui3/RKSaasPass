package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureFinanceSchema_shouldCreateMissingTables() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record'"), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account'"), eq(Integer.class))).thenReturn(0);

        new FinanceSchemaUpdater(jdbcTemplate).ensureFinanceSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE finance_record"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE finance_account"));
    }

    @Test
    void ensureFinanceSchema_shouldBackfillEnterpriseColumns() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("tenant_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("type"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("amount"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("category"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("record_no"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("business_type"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("business_id"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("budget_item"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("period"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("title"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("description"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("proof_image_url"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("operator_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("reviewer_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("reject_reason"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("posted_status"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("voucher_id"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("status"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("review_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("create_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("update_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_record' AND COLUMN_NAME = ?"), eq(Integer.class), eq("is_deleted"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("tenant_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("balance"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("total_income"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("total_expense"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("create_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("update_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'finance_account' AND COLUMN_NAME = ?"), eq(Integer.class), eq("is_deleted"))).thenReturn(1);

        new FinanceSchemaUpdater(jdbcTemplate).ensureFinanceSchema();

        verify(jdbcTemplate).execute(contains("ADD COLUMN record_no"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN business_type"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN business_id"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN budget_item"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN period"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN reject_reason"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN posted_status"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN voucher_id"));
    }
}
