package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.GrowthRetentionCenterVO;
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

class GrowthRetentionCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedGrowthRetentionWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        GrowthRetentionCenterServiceImpl service = new GrowthRetentionCenterServiceImpl(jdbcTemplate, redisTemplate);

        GrowthRetentionCenterVO result = service.getGrowthRetentionCenter(99L, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertEquals("90D", result.getFilters().getTimeRange());
        assertEquals("ALL", result.getFilters().getRiskLevel());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getMetrics().isEmpty());
        assertFalse(result.getTrendRows().isEmpty());
        assertFalse(result.getRiskRows().isEmpty());
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("growth")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmupAllTenantsStoresGrowthRetentionCacheWithJitter() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("rk_tenant", "club_members", "sys_logininfor", "volunteer_record", "user_credit_record", "user_credit_summary")
                            .contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(1);

        GrowthRetentionCenterServiceImpl service = new GrowthRetentionCenterServiceImpl(jdbcTemplate, redisTemplate);

        List<GrowthRetentionCenterVO> warmed = service.warmupAllTenants();

        assertEquals(2, warmed.size());
        assertEquals(7L, warmed.get(0).getTenantId());
        assertEquals(8L, warmed.get(1).getTenantId());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("DATA:TENANT:GROWTH_RETENTION_CENTER:7:90D:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("DATA:TENANT:GROWTH_RETENTION_CENTER:8:90D:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttlCaptor.getAllValues().stream().allMatch(ttl -> ttl >= 240 && ttl <= 360));
    }

    @Test
    void keepsMonthlyTrendAndRiskDiagnosticsForAvailableTables() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("club_members", "sys_logininfor", "volunteer_record", "user_credit_record", "user_credit_summary")
                            .contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(3);

        GrowthRetentionCenterServiceImpl service = new GrowthRetentionCenterServiceImpl(jdbcTemplate, redisTemplate);

        GrowthRetentionCenterVO result = service.getGrowthRetentionCenter(8L, "30D", "MEDIUM");

        assertEquals("30D", result.getFilters().getTimeRange());
        assertEquals("MEDIUM", result.getFilters().getRiskLevel());
        assertNotNull(result.getMonthlyTrend());
        assertTrue(result.getSummary().getRetentionRate() >= 0);
        assertTrue(result.getTrendRows().stream().anyMatch(row -> row.getMonth() != null));
        assertTrue(result.getSources().stream().anyMatch(source -> source.getTableName().contains("rk_user")));
    }
}
