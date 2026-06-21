package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumniGraduationSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureAlumniGraduationSchema_shouldCreateTokenAndLogTablesWhenMissing() {
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class), eq("alumni_profile_token")))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class), eq("alumni_graduation_log")))
                .thenReturn(0);

        AlumniGraduationSchemaUpdater updater = new AlumniGraduationSchemaUpdater(jdbcTemplate);

        updater.ensureAlumniGraduationSchema();

        verify(jdbcTemplate).execute(contains("CREATE TABLE alumni_profile_token"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE alumni_graduation_log"));
        verify(jdbcTemplate).execute(contains("UNIQUE KEY uk_alumni_profile_token"));
        verify(jdbcTemplate).execute(contains("UNIQUE KEY uk_alumni_graduation_member_year_action"));
    }

    @Test
    void ensureAlumniGraduationSchema_shouldSkipExistingTables() {
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class), eq("alumni_profile_token")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class), eq("alumni_graduation_log")))
                .thenReturn(1);

        AlumniGraduationSchemaUpdater updater = new AlumniGraduationSchemaUpdater(jdbcTemplate);

        updater.ensureAlumniGraduationSchema();

        verify(jdbcTemplate, times(2)).queryForObject(contains("information_schema.TABLES"), eq(Integer.class), org.mockito.ArgumentMatchers.anyString());
        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
