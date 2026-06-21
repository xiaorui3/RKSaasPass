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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumniSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureAlumniSchema_shouldAddMissingColumnsAndBackfillStatuses() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'club_alumni' AND COLUMN_NAME = ?"), eq(Integer.class), anyString()))
                .thenReturn(0);

        AlumniSchemaUpdater updater = new AlumniSchemaUpdater(jdbcTemplate);

        updater.ensureAlumniSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE club_alumni ADD COLUMN show_table"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE club_alumni ADD COLUMN is_active"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE club_alumni ADD COLUMN is_core_member"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE club_alumni ADD COLUMN member_status"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE club_alumni ADD COLUMN graduation_status"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("UPDATE club_alumni"));
    }
}
