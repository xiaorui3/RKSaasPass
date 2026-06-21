package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ApprovalTaskCenterVO;
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

class ApprovalTaskCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedApprovalTasksWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        ApprovalTaskCenterServiceImpl service = new ApprovalTaskCenterServiceImpl(jdbcTemplate, redisTemplate);

        ApprovalTaskCenterVO result = service.getApprovalTaskCenter(99L, null, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getSummary());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getCache().isHit());
        assertFalse(result.getTasks().isEmpty());
        assertTrue(result.getTasks().stream().anyMatch(task -> "SOURCE_MISSING".equals(task.getStatus())));
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("approval")));
    }

    @Test
    void superAdminMayRequestAllTenantApprovalTasks() {
        TenantContext.setTenantId(8L);
        TenantContext.setSuperAdmin(true);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        ApprovalTaskCenterServiceImpl service = new ApprovalTaskCenterServiceImpl(jdbcTemplate, redisTemplate);

        ApprovalTaskCenterVO result = service.getApprovalTaskCenter(0L, "activity", "OVERDUE", "PENDING");

        assertNull(result.getTenantId());
        assertEquals("ALL", result.getTenantScope());
        assertTrue(result.getCache().getKey().contains("ALL"));
        assertEquals("ACTIVITY", result.getFilters().getTaskType());
        assertEquals("OVERDUE", result.getFilters().getSlaStatus());
        assertEquals("PENDING", result.getFilters().getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmupAllTenantsStoresApprovalCenterCacheWithJitter() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return List.of("rk_tenant", "join_requests").contains(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L, 8L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 1L, "title", "待审核入社", "status", "PENDING")));

        ApprovalTaskCenterServiceImpl service = new ApprovalTaskCenterServiceImpl(jdbcTemplate, redisTemplate);

        List<ApprovalTaskCenterVO> warmed = service.warmupAllTenants();

        assertEquals(2, warmed.size());
        assertEquals(7L, warmed.get(0).getTenantId());
        assertEquals(8L, warmed.get(1).getTenantId());
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("DATA:TENANT:APPROVAL_TASK_CENTER:7:ALL:ALL:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("DATA:TENANT:APPROVAL_TASK_CENTER:8:ALL:ALL:ALL"), anyString(), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertTrue(ttlCaptor.getAllValues().stream().allMatch(ttl -> ttl >= 120 && ttl <= 180));
    }

    @Test
    void keepsPendingConditionWhenOnlyReviewStatusColumnExists() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                .thenAnswer(invocation -> {
                    Object table = invocation.getArgument(3);
                    return "register_review_request".equals(String.valueOf(table)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Object column = invocation.getArgument(4);
                    return List.of("id", "review_status", "tenant_id", "create_time").contains(String.valueOf(column)) ? 1 : 0;
                });
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 1L, "review_status", "PENDING")));

        ApprovalTaskCenterServiceImpl service = new ApprovalTaskCenterServiceImpl(jdbcTemplate, redisTemplate);

        ApprovalTaskCenterVO result = service.getApprovalTaskCenter(8L, "ADMISSION", "ALL", "ALL");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), org.mockito.ArgumentMatchers.<Object[]>any());
        assertTrue(sqlCaptor.getAllValues().stream().anyMatch(sql -> sql.contains("review_status IN")));
        assertEquals(1, result.getTasks().stream()
                .filter(task -> "admission".equals(task.getTaskType()))
                .filter(task -> "PENDING".equals(task.getStatus()))
                .count());
        assertTrue(result.getSources().stream()
                .anyMatch(source -> "approval.admission".equals(source.getMetric()) && source.getStatus().equals("ok")));
    }
}
