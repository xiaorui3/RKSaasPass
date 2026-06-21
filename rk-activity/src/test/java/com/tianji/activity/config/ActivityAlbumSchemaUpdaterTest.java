package com.tianji.activity.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityAlbumSchemaUpdaterTest {

    @Test
    void ensureActivityAlbumTable_shouldCreateTableWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        new ActivityAlbumSchemaUpdater(jdbcTemplate).ensureActivityAlbumTable();

        verify(jdbcTemplate).execute(contains("CREATE TABLE rk_activity_album_photo"));
    }

    @Test
    void ensureActivityAlbumTable_shouldNotThrowWhenMigrationFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenThrow(new RuntimeException("DDL denied"));

        assertDoesNotThrow(() -> new ActivityAlbumSchemaUpdater(jdbcTemplate).ensureActivityAlbumTable());
    }

    @Test
    void ensureActivityAlbumTable_shouldAddMissingColumnsWhenTableExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("file_name"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("description"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("sort_order"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("uploader_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("tenant_id"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("create_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("update_time"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("creator"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("updater"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("is_deleted"))).thenReturn(1);

        new ActivityAlbumSchemaUpdater(jdbcTemplate).ensureActivityAlbumTable();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("ADD COLUMN file_name"));
    }
}
