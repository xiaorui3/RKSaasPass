package com.tianji.data.service.impl;

import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.FrontendCacheSnapshotProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontendCacheWarmupServiceImplTest {

    @Test
    void ttlSecondsAddsJitterWithinExpectedRange() {
        long first = FrontendCacheWarmupServiceImpl.ttlSeconds(false);
        long second = FrontendCacheWarmupServiceImpl.ttlSeconds(false);

        assertThat(first).isBetween(3600L, 4200L);
        assertThat(second).isBetween(3600L, 4200L);
    }

    @Test
    void buildTenantKeyIncludesTenantAndVersion() {
        String key = FrontendCacheWarmupServiceImpl.buildTenantKey(42L, "news");

        assertThat(key).isEqualTo("FRONTEND:CACHE:tenant:42:v1:news");
    }

    @Test
    void manualWarmupStoresEmptyResultWithShortTtl() {
        StringRedisTemplate redisTemplate = mockRedisTemplate();
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        FrontendCacheSnapshotProvider provider = mock(FrontendCacheSnapshotProvider.class);
        when(provider.listTenantIds()).thenReturn(Collections.singletonList(7L));
        when(provider.loadVisibleContent(7L)).thenReturn(Collections.singletonList(
                new FrontendCacheWarmupServiceImpl.FrontendCacheItem("news", Collections.emptyList(), true)
        ));

        FrontendCacheWarmupServiceImpl service = new FrontendCacheWarmupServiceImpl(redisTemplate, provider);
        service.manualWarmup();

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOps).set(eq("FRONTEND:CACHE:tenant:7:v1:news"), eq("[]"), ttlCaptor.capture(), eq(TimeUnit.SECONDS));
        assertThat(ttlCaptor.getValue()).isBetween(120L, 240L);
    }

    @Test
    void checkAndWarmupDegradesWhenRedisUnavailable() {
        StringRedisTemplate redisTemplate = mockRedisTemplate();
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        doThrow(new RuntimeException("down")).when(connection).ping();
        FrontendCacheSnapshotProvider provider = mock(FrontendCacheSnapshotProvider.class);

        FrontendCacheWarmupServiceImpl service = new FrontendCacheWarmupServiceImpl(redisTemplate, provider);
        FrontendCacheWarmupSummaryVO summary = service.checkAndWarmup();

        assertThat(summary.isDegraded()).isTrue();
        assertThat(summary.isRedisAvailable()).isFalse();
        assertThat(summary.getReason()).contains("redis unavailable");
    }

    @Test
    void defaultProviderLoadsBusinessSnapshotsFromCrossServiceDatabases() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    Object table = invocation.getArgument(3);
                    if (sql.contains("information_schema.tables")) {
                        return List.of("rk_tenant", "news", "rk_activity").contains(String.valueOf(table)) ? 1 : 0;
                    }
                    return 1;
                });
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), eq(Long.class)))
                .thenReturn(List.of(7L));
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_user`.`rk_tenant`"), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 7L, "tenant_name", "黑河学院计算机社团")));
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_content`.`news`"), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 1L, "title", "黑河学院创新创业活动")));
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("`rk_activity`.`rk_activity`"), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("id", 2L, "activity_name", "黑河学院志愿服务")));

        DefaultFrontendCacheSnapshotProvider provider = new DefaultFrontendCacheSnapshotProvider(jdbcTemplate);

        List<Long> tenantIds = provider.listTenantIds();
        List<FrontendCacheWarmupServiceImpl.FrontendCacheItem> items = provider.loadVisibleContent(7L);

        assertThat(tenantIds).containsExactly(7L);
        assertThat(items).extracting(FrontendCacheWarmupServiceImpl.FrontendCacheItem::getContentType)
                .contains("tenant-public", "news", "activities", "summary");
        assertThat(items).noneMatch(item -> "visible frontend cache warmup placeholder".equals(item.getPayload()));
        assertThat(items.stream()
                .filter(item -> "news".equals(item.getContentType()))
                .findFirst()
                .map(FrontendCacheWarmupServiceImpl.FrontendCacheItem::getPayload)
                .orElseGet(ArrayList::new).toString()).contains("黑河学院创新创业活动");
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate mockRedisTemplate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.hasKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        return redisTemplate;
    }
}
