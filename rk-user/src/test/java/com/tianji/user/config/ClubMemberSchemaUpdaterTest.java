package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMemberSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureClubMemberSchema_shouldReplaceGlobalStudentUniqueIndexWithTenantScopedIndex() {
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.STATISTICS"), eq(Integer.class), eq("uk_tenant_student_id"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("information_schema.STATISTICS"), eq(Integer.class), eq("uk_student_id"))).thenReturn(1);

        ClubMemberSchemaUpdater updater = new ClubMemberSchemaUpdater(jdbcTemplate);

        updater.ensureClubMemberSchema();

        verify(jdbcTemplate).execute(contains("UPDATE club_members SET tenant_id = 1"));
        verify(jdbcTemplate).execute(contains("MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1"));
        verify(jdbcTemplate).execute(contains("ADD UNIQUE KEY `uk_tenant_student_id`"));
        verify(jdbcTemplate).execute(contains("DROP INDEX `uk_student_id`"));
    }

    @Test
    void ensureClubMemberSchema_shouldSkipIndexesWhenTableMissing() {
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class))).thenReturn(0);

        ClubMemberSchemaUpdater updater = new ClubMemberSchemaUpdater(jdbcTemplate);

        updater.ensureClubMemberSchema();

        verify(jdbcTemplate, never()).execute(contains("club_members"));
    }
}
