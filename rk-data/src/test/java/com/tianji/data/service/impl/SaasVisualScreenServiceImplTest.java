package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.SaasVisualScreenVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SaasVisualScreenServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void returnsTenantScopedSummaryWithCacheMetadataWhenRedisUnavailable() {
        TenantContext.setTenantId(7L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        SaasVisualScreenServiceImpl service = new SaasVisualScreenServiceImpl(jdbcTemplate, redisTemplate);

        SaasVisualScreenVO result = service.getOverview(99L);

        assertEquals(7L, result.getTenantId());
        assertFalse(result.getCache().isHit());
        assertEquals("fallback", result.getCache().getMode());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void superAdminMayRequestAllPlatformScopeAndCacheKeyContainsTenantScope() {
        TenantContext.setTenantId(7L);
        TenantContext.setSuperAdmin(true);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        SaasVisualScreenServiceImpl service = new SaasVisualScreenServiceImpl(jdbcTemplate, redisTemplate);

        SaasVisualScreenVO result = service.getOverview(0L);

        assertNull(result.getTenantId());
        assertEquals("ALL", result.getTenantScope());
        assertTrue(result.getCache().getKey().contains("ALL"));
    }

    @Test
    void searchHealthFallsBackWhenSearchIndexTableIsMissing() {
        TenantContext.setTenantId(7L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1) FROM information_schema.tables"), eq(Integer.class), any(), any()))
                .thenReturn(0);

        SaasVisualScreenServiceImpl service = new SaasVisualScreenServiceImpl(jdbcTemplate, redisTemplate);

        SaasVisualScreenVO result = service.getOverview(null);

        assertEquals("MISSING", result.getSearchHealth().getIndexStatus());
        assertEquals("rk_global_search", result.getSearchHealth().getIndexName());
        assertEquals(0L, result.getSearchHealth().getDocumentCount());
        assertTrue(result.getSources().stream().anyMatch(item ->
                "searchHealth".equals(item.getMetric()) && item.getStatus().contains("missing")));
    }

    @Test
    void cacheSnapshotScansFrontendCacheKeysWithoutRedisKeysCommand() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        Cursor<byte[]> cursor = mock(Cursor.class);
        Iterator<byte[]> iterator = java.util.List.of(
                "rk:frontend-cache:tenant:7:warmup:/api/news".getBytes(StandardCharsets.UTF_8),
                "rk:frontend-cache:null:tenant:7:/api/alumni".getBytes(StandardCharsets.UTF_8)
        ).iterator();
        when(cursor.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        when(cursor.next()).thenAnswer(invocation -> iterator.next());
        RedisConnection connection = mock(RedisConnection.class);
        doReturn(cursor).when(connection).scan(any(ScanOptions.class));

        SaasVisualScreenServiceImpl service = new SaasVisualScreenServiceImpl(jdbcTemplate, redisTemplate);
        SaasVisualScreenVO.CacheSnapshot snapshot = new SaasVisualScreenVO.CacheSnapshot();

        service.scanFrontendCacheSnapshot(snapshot, connection, key ->
                key.contains(":null:") || key.contains("/api/alumni") ? "__RK_NULL__" : "{\"code\":200}");
        service.completeFrontendCacheSnapshot(snapshot);

        assertEquals(2L, snapshot.getTotalKeys());
        assertEquals(1L, snapshot.getNullMarkerCount());
        assertEquals(1L, snapshot.getWarmupKeyCount());
        assertEquals(0L, snapshot.getNormalKeyCount());
        assertTrue(snapshot.getTotalSizeText().contains("2 keys"));
    }
}
