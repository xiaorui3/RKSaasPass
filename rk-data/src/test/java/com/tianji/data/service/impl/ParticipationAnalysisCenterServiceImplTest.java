package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ParticipationAnalysisCenterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipationAnalysisCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedParticipationAnalysisWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        ParticipationAnalysisCenterServiceImpl service = new ParticipationAnalysisCenterServiceImpl(jdbcTemplate, redisTemplate);

        ParticipationAnalysisCenterVO result = service.getParticipationAnalysisCenter(99L, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getSummary());
        assertEquals("ALL", result.getFilters().getDimension());
        assertEquals("30D", result.getFilters().getTimeRange());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getMetrics().isEmpty());
        assertFalse(result.getTrendRows().isEmpty());
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("participation")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmupAllTenantsStoresParticipationCacheWithJitter() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("rk_tenant", "club_members", "rk_activity_registration", "competition_participants", "news", "works", "comments", "sys_logininfor")
                            .contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(1);

        ParticipationAnalysisCenterServiceImpl service = new ParticipationAnalysisCenterServiceImpl(jdbcTemplate, redisTemplate);

        List<ParticipationAnalysisCenterVO> warmed = service.warmupAllTenants();

        assertEquals(2, warmed.size());
        assertEquals(7L, warmed.get(0).getTenantId());
        assertEquals(8L, warmed.get(1).getTenantId());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("DATA:TENANT:PARTICIPATION_ANALYSIS_CENTER:7:ALL:30D"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("DATA:TENANT:PARTICIPATION_ANALYSIS_CENTER:8:ALL:30D"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttlCaptor.getAllValues().stream().allMatch(ttl -> ttl >= 180 && ttl <= 270));
    }

    @Test
    void keepsEngagementTrendAndSourceDiagnosticsForAvailableTables() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("rk_activity_registration", "competition_participants", "news", "works", "comments", "club_members", "sys_logininfor")
                            .contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(3);

        ParticipationAnalysisCenterServiceImpl service = new ParticipationAnalysisCenterServiceImpl(jdbcTemplate, redisTemplate);

        ParticipationAnalysisCenterVO result = service.getParticipationAnalysisCenter(8L, "content", "7D");

        assertEquals("CONTENT", result.getFilters().getDimension());
        assertEquals("7D", result.getFilters().getTimeRange());
        assertNotNull(result.getEngagementTrend());
        assertTrue(result.getTrendRows().stream().allMatch(row -> "content".equals(row.getDimension())));
        assertTrue(result.getSummary().getRegistrationConversionRate() >= 0);
        assertTrue(result.getSources().stream().anyMatch(source -> source.getTableName().contains("rk_content")));
    }
}
