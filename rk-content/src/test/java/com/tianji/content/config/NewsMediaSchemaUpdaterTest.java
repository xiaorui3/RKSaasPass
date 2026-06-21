package com.tianji.content.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsMediaSchemaUpdaterTest {

    @Test
    void ensureNewsMediaColumns_shouldNotThrowWhenMigrationFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenThrow(new RuntimeException("ALTER denied"));

        NewsMediaSchemaUpdater updater = new NewsMediaSchemaUpdater(jdbcTemplate);

        assertDoesNotThrow(updater::ensureNewsMediaColumns);
    }
}
