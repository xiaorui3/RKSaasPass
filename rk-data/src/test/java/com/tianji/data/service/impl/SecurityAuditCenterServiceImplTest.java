package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.dto.SecurityAuditNotifyRequestDTO;
import com.tianji.data.model.vo.SecurityAuditCenterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityAuditCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedSecurityRisksWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);

        SecurityAuditCenterServiceImpl service = new SecurityAuditCenterServiceImpl(jdbcTemplate, redisTemplate);

        SecurityAuditCenterVO result = service.getSecurityAuditCenter(99L, null, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getSummary());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getRisks().isEmpty());
        assertTrue(result.getRisks().stream().anyMatch(risk -> "SOURCE_MISSING".equals(risk.getStatus())));
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("security")));
    }

    @Test
    void notifyRiskRegistersAuditLogWithoutCrossServiceSend() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SecurityAuditCenterServiceImpl service = new SecurityAuditCenterServiceImpl(jdbcTemplate, redisTemplate);

        SecurityAuditNotifyRequestDTO request = new SecurityAuditNotifyRequestDTO()
                .setRiskType("high_risk_operation")
                .setRiskLevel("HIGH")
                .setReason("manual test")
                .setAudience("tenant-admin")
                .setOperator("tester");

        SecurityAuditCenterVO.NotifyLog log = service.notifyRisk(request);

        assertEquals(8L, log.getTenantId());
        assertEquals("high_risk_operation", log.getRiskType());
        assertEquals("HIGH", log.getRiskLevel());
        assertEquals("REGISTERED", log.getStatus());
        assertTrue(log.getMessage().contains("登记"));
        verify(jdbcTemplate).update(contains("security_audit_notify_log"), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
