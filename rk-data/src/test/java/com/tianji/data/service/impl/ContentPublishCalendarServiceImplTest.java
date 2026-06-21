package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ContentPublishCalendarVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentPublishCalendarServiceImplTest {

    private static final Set<String> TABLES = Set.of(
            "rk_tenant", "news", "notice", "notices", "works", "rk_activity", "competition_competitions"
    );
    private static final Set<String> COLUMNS = Set.of(
            "id", "title", "name", "notice_title", "activity_name",
            "publish_time", "create_time", "update_time", "start_time", "registration_start_time",
            "registration_start", "competition_start",
            "is_published", "approval_status", "manager_review_status", "teacher_review_status",
            "activity_status", "status", "tenant_id", "is_deleted"
    );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedContentCalendarWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        stubInformationSchema(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), ArgumentMatchers.<Object[]>any())).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(
                        Map.of("id", 1L, "title", "Heihe College club news", "date_value", "2026-06-17 09:00:00", "status_value", "PUBLISHED")
                ));

        ContentPublishCalendarServiceImpl service = new ContentPublishCalendarServiceImpl(jdbcTemplate, redisTemplate);

        ContentPublishCalendarVO result = service.getContentPublishCalendar(99L, null, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertEquals("30D", result.getFilters().getTimeRange());
        assertEquals("ALL", result.getFilters().getContentType());
        assertEquals("ALL", result.getFilters().getStatus());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getSummaryCards().isEmpty());
        assertFalse(result.getCalendarRows().isEmpty());
        assertFalse(result.getTimelineRows().isEmpty());
        assertFalse(result.getRiskRows().isEmpty());
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("calendar")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmupAllTenantsStoresContentCalendarCacheWithJitter() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        stubInformationSchema(jdbcTemplate);
        when(jdbcTemplate.queryForList(ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), ArgumentMatchers.<Object[]>any())).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 1L, "title", "Heihe College publish item", "date_value", "2026-06-17 09:00:00", "status_value", "PUBLISHED")));

        ContentPublishCalendarServiceImpl service = new ContentPublishCalendarServiceImpl(jdbcTemplate, redisTemplate);

        List<ContentPublishCalendarVO> warmed = service.warmupAllTenants();

        assertEquals(2, warmed.size());
        assertEquals(7L, warmed.get(0).getTenantId());
        assertEquals(8L, warmed.get(1).getTenantId());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("DATA:TENANT:CONTENT_PUBLISH_CALENDAR:7:30D:ALL:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("DATA:TENANT:CONTENT_PUBLISH_CALENDAR:8:30D:ALL:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttlCaptor.getAllValues().stream().allMatch(ttl -> ttl >= 210 && ttl <= 330));
    }

    @Test
    void aggregatesCalendarTimelineAndRiskDiagnosticsForAvailableTables() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        stubInformationSchema(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), ArgumentMatchers.<Object[]>any())).thenReturn(3);
        when(jdbcTemplate.queryForList(anyString(), ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(
                        Map.of("id", 1L, "title", "Heihe College activity", "date_value", "2026-06-17 09:00:00", "status_value", "PUBLISHED"),
                        Map.of("id", 2L, "title", "Heihe College scheduled activity", "date_value", "2026-06-19 10:00:00", "status_value", "SCHEDULED")
                ));

        ContentPublishCalendarServiceImpl service = new ContentPublishCalendarServiceImpl(jdbcTemplate, redisTemplate);

        ContentPublishCalendarVO result = service.getContentPublishCalendar(8L, "7D", "activity", "SCHEDULED");

        assertEquals("7D", result.getFilters().getTimeRange());
        assertEquals("ACTIVITY", result.getFilters().getContentType());
        assertEquals("SCHEDULED", result.getFilters().getStatus());
        assertTrue(result.getSummary().getPublishedCount() >= 0);
        assertTrue(result.getCalendarRows().stream().anyMatch(row -> row.getDate() != null));
        assertTrue(result.getTimelineRows().stream().anyMatch(row -> "activity".equals(row.getContentType())));
        assertTrue(result.getRiskRows().stream().anyMatch(row -> row.getRiskType() != null));
        assertTrue(result.getSources().stream().anyMatch(source -> source.getTableName().contains("rk_")));
    }

    private void stubInformationSchema(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForObject(ArgumentMatchers.contains("information_schema.tables"), eq(Integer.class), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return TABLES.contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(ArgumentMatchers.contains("information_schema.columns"), eq(Integer.class), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Object column = invocation.getArgument(4);
                    return COLUMNS.contains(String.valueOf(column)) ? 1 : 0;
                });
    }
}
