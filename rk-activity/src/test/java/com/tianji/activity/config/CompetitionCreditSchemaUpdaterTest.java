package com.tianji.activity.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompetitionCreditSchemaUpdaterTest {

    @Test
    void ensureCompetitionCreditColumns_shouldCreateMissingColumns() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);

        CompetitionCreditSchemaUpdater updater = new CompetitionCreditSchemaUpdater(jdbcTemplate);

        updater.ensureCompetitionCreditColumns();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE competition_competitions ADD COLUMN participation_points"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE competition_competitions ADD COLUMN first_prize_points"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE competition_competitions ADD COLUMN second_prize_points"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE competition_competitions ADD COLUMN third_prize_points"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE competition_competitions ADD COLUMN excellent_prize_points"));
    }

    @Test
    void ensureCompetitionCreditColumns_shouldNotThrowWhenMigrationFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenThrow(new RuntimeException("ALTER denied"));

        CompetitionCreditSchemaUpdater updater = new CompetitionCreditSchemaUpdater(jdbcTemplate);

        assertDoesNotThrow(updater::ensureCompetitionCreditColumns);
    }
}
