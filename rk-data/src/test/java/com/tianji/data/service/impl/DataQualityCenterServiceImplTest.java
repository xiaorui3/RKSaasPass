package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.dto.DataQualityRepairRequestDTO;
import com.tianji.data.model.vo.DataQualityCenterVO;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.FrontendCacheWarmupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataQualityCenterServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedQualityIssuesWhenRedisUnavailable() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Number.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0);

        DataQualityCenterServiceImpl service = new DataQualityCenterServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        DataQualityCenterVO result = service.getDataQualityCenter(99L, null, null);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getSummary());
        assertEquals("fallback", result.getCache().getMode());
        assertFalse(result.getIssues().isEmpty());
        assertTrue(result.getIssues().stream().anyMatch(issue -> "SOURCE_MISSING".equals(issue.getStatus())));
        assertTrue(result.getSources().stream().anyMatch(source -> source.getMetric().contains("quality")));
    }

    @Test
    void repairRedisWarmupIsAuditedAndDelegatesToWarmupService() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(warmupService.manualWarmup()).thenReturn(FrontendCacheWarmupSummaryVO.builder()
                .redisAvailable(true)
                .warmed(true)
                .checkedAt(LocalDateTime.now())
                .build());

        DataQualityCenterServiceImpl service = new DataQualityCenterServiceImpl(jdbcTemplate, redisTemplate, warmupService);
        DataQualityRepairRequestDTO request = new DataQualityRepairRequestDTO()
                .setIssueType("redis")
                .setReason("manual test");

        DataQualityCenterVO.RepairLog log = service.repair(request);

        assertEquals(8L, log.getTenantId());
        assertEquals("redis", log.getIssueType());
        assertEquals("SUCCESS", log.getStatus());
        assertTrue(log.getMessage().contains("Redis"));
        verify(warmupService).manualWarmup();
        verify(jdbcTemplate).update(contains("data_quality_repair_log"), any(), any(), any(), any(), any(), any());
    }
}
