package com.tianji.message.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeMediaSchemaUpdaterTest {

    @Test
    void ensureNoticeMediaColumns_shouldNotThrowWhenMigrationFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenThrow(new RuntimeException("ALTER denied"));

        NoticeMediaSchemaUpdater updater = new NoticeMediaSchemaUpdater(jdbcTemplate);

        assertDoesNotThrow(updater::ensureNoticeMediaColumns);
    }

    @Test
    void ensureNoticeMediaColumns_shouldRepairPublishColumnsRequiredByNoticeEntity() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);

        NoticeMediaSchemaUpdater updater = new NoticeMediaSchemaUpdater(jdbcTemplate);

        updater.ensureNoticeMediaColumns();

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN is_published"));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN target_ids"));
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD COLUMN notice_level"));
    }
}
