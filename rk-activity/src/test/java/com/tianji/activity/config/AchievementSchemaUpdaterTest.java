package com.tianji.activity.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureAchievementSchema_shouldCreateTableWhenMissing() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'rk_member_achievement'"), eq(Integer.class))).thenReturn(0);

        AchievementSchemaUpdater updater = new AchievementSchemaUpdater(jdbcTemplate);

        updater.ensureAchievementSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE rk_member_achievement"));
    }

    @Test
    void ensureAchievementSchema_shouldAddReviewColumnsWhenTableExists() {
        when(jdbcTemplate.queryForObject(contains("information_schema.TABLES"), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.COLUMNS"), eq(Integer.class), anyString())).thenReturn(0);

        AchievementSchemaUpdater updater = new AchievementSchemaUpdater(jdbcTemplate);

        updater.ensureAchievementSchema();

        verify(jdbcTemplate).execute(contains("ADD COLUMN manager_review_status"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN teacher_reviewer_id"));
    }
}
