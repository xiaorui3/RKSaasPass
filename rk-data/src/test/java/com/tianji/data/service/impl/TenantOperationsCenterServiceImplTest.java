package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.TenantOperationsCenterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantOperationsCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedOperationsCenterWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Object[].class))).thenReturn(0);

        TenantOperationsCenterServiceImpl service = new TenantOperationsCenterServiceImpl(jdbcTemplate, redisTemplate);

        TenantOperationsCenterVO result = service.getOverview(99L);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getHealthScore());
        assertTrue(result.getHealthScore().getScore() >= 0);
        assertTrue(result.getHealthScore().getScore() <= 100);
        assertNotNull(result.getApprovalCenter());
        assertNotNull(result.getParticipation());
        assertNotNull(result.getDataQuality());
        assertNotNull(result.getGrowthRetention());
        assertNotNull(result.getSecurityAudit());
        assertFalse(result.getApprovalDetails().isEmpty());
        assertFalse(result.getQualityDetails().isEmpty());
        assertFalse(result.getSecurityDetails().isEmpty());
        assertTrue(result.getApprovalDetails().stream().anyMatch(detail -> "approval".equals(detail.getType())));
        assertTrue(result.getQualityDetails().stream().anyMatch(detail -> "quality".equals(detail.getType())));
        assertTrue(result.getSecurityDetails().stream().anyMatch(detail -> "security".equals(detail.getType())));
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getCache().isHit());
        assertTrue(result.getSources().stream().anyMatch(source -> "healthScore".equals(source.getMetric())));
    }

    @Test
    void superAdminMayRequestAllTenantOperations() {
        TenantContext.setTenantId(8L);
        TenantContext.setSuperAdmin(true);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Object[].class))).thenReturn(0);

        TenantOperationsCenterServiceImpl service = new TenantOperationsCenterServiceImpl(jdbcTemplate, redisTemplate);

        TenantOperationsCenterVO result = service.getOverview(0L);

        assertNull(result.getTenantId());
        assertEquals("ALL", result.getTenantScope());
        assertTrue(result.getCache().getKey().contains("ALL"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmupAllTenantsStoresTenantOperationsCache() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("rk_tenant", "club_members").contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 1L, "title", "待处理事项", "status", "PENDING")));

        TenantOperationsCenterServiceImpl service = new TenantOperationsCenterServiceImpl(jdbcTemplate, redisTemplate);

        List<TenantOperationsCenterVO> warmed = service.warmupAllTenants();

        assertEquals(2, warmed.size());
        assertEquals(7L, warmed.get(0).getTenantId());
        assertEquals(8L, warmed.get(1).getTenantId());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("DATA:TENANT:OPERATIONS_CENTER:7"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("DATA:TENANT:OPERATIONS_CENTER:8"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttlCaptor.getAllValues().stream().allMatch(ttl -> ttl >= 180 && ttl <= 240));
    }
}
