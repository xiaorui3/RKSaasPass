package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureTenantSchema_shouldAddDisplayOrderWhenColumnMissing() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'rk_tenant'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("COLUMN_NAME = ?"), eq(Integer.class), anyString())).thenReturn(0);

        TenantSchemaUpdater updater = new TenantSchemaUpdater(jdbcTemplate);

        updater.ensureTenantSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("ADD COLUMN `display_order`"));
    }

    @Test
    void ensureTenantSchema_shouldSkipWhenDisplayOrderAlreadyExists() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'rk_tenant'"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("COLUMN_NAME = ?"), eq(Integer.class), anyString())).thenReturn(1);

        TenantSchemaUpdater updater = new TenantSchemaUpdater(jdbcTemplate);

        updater.ensureTenantSchema();

        verify(jdbcTemplate, never()).execute(contains("ADD COLUMN `display_order`"));
    }
}
