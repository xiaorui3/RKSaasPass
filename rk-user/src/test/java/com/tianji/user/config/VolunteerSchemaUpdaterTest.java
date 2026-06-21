package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureVolunteerSchema_shouldBackfillAuditColumns() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'volunteer_record'"), eq(Integer.class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'volunteer_record' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("creator")
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                contains("TABLE_NAME = 'volunteer_record' AND COLUMN_NAME = ?"),
                eq(Integer.class),
                eq("updater")
        )).thenReturn(0);

        new VolunteerSchemaUpdater(jdbcTemplate).ensureVolunteerSchema();

        verify(jdbcTemplate).execute(contains("ALTER TABLE volunteer_record ADD COLUMN creator"));
        verify(jdbcTemplate).execute(contains("ALTER TABLE volunteer_record ADD COLUMN updater"));
    }
}
