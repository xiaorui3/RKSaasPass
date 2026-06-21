package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.FrontendCacheSnapshotProvider;
import com.tianji.data.service.FrontendCacheWarmupService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontendCacheWarmupServiceImpl implements FrontendCacheWarmupService {

    public static final String KEY_PREFIX = "FRONTEND:CACHE:tenant:";
    public static final String KEY_VERSION = "v1";
    public static final String SUMMARY_KEY = "FRONTEND:CACHE:WARMUP:SUMMARY";
    public static final long NORMAL_TTL_SECONDS = 3600L;
    public static final long EMPTY_TTL_SECONDS = 120L;
    public static final long TTL_JITTER_SECONDS = 600L;

    private final StringRedisTemplate redisTemplate;
    private final FrontendCacheSnapshotProvider snapshotProvider;

    private volatile FrontendCacheWarmupSummaryVO lastSummary = degradedSummary("not checked");

    @Override
    public FrontendCacheWarmupSummaryVO checkAndWarmup() {
        long start = System.currentTimeMillis();
        FrontendCacheWarmupSummaryVO summary = baseSummary(start, "check");
        if (!isRedisAvailable(summary)) {
            return finish(summary, start);
        }

        List<Long> tenantIds = safeTenantIds(summary);
        summary.setTenantCount(tenantIds.size());
        for (Long tenantId : tenantIds) {
            String key = buildTenantKey(tenantId, "summary");
            summary.getCheckedKeys().add(key);
            try {
                Boolean hit = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hit)) {
                    summary.setHitCount(summary.getHitCount() + 1);
                } else {
                    summary.setMissCount(summary.getMissCount() + 1);
                }
            } catch (Exception e) {
                markRedisFailure(summary, "check key failed: " + e.getMessage());
                return finish(summary, start);
            }
        }

        if (summary.getMissCount() > 0) {
            warmupTenants(summary, tenantIds);
        }
        return finish(summary, start);
    }

    @Override
    public FrontendCacheWarmupSummaryVO manualWarmup() {
        long start = System.currentTimeMillis();
        FrontendCacheWarmupSummaryVO summary = baseSummary(start, "manual");
        if (!isRedisAvailable(summary)) {
            return finish(summary, start);
        }
        List<Long> tenantIds = safeTenantIds(summary);
        summary.setTenantCount(tenantIds.size());
        warmupTenants(summary, tenantIds);
        return finish(summary, start);
    }

    @Override
    public FrontendCacheWarmupSummaryVO latestSummary() {
        try {
            String cached = redisTemplate.opsForValue().get(SUMMARY_KEY);
            if (cached != null) {
                FrontendCacheWarmupSummaryVO summary = JsonUtils.toBean(cached, FrontendCacheWarmupSummaryVO.class);
                if (summary != null) {
                    return summary;
                }
            }
        } catch (Exception e) {
            return degradedSummary("redis unavailable: " + e.getMessage());
        }
        return lastSummary;
    }

    public static String buildTenantKey(Long tenantId, String contentType) {
        return KEY_PREFIX + tenantId + ":" + KEY_VERSION + ":" + contentType;
    }

    public static long ttlSeconds(boolean emptyResult) {
        long base = emptyResult ? EMPTY_TTL_SECONDS : NORMAL_TTL_SECONDS;
        long jitter = ThreadLocalRandom.current().nextLong(TTL_JITTER_SECONDS + 1);
        return emptyResult ? base + Math.min(jitter, EMPTY_TTL_SECONDS) : base + jitter;
    }

    private boolean isRedisAvailable(FrontendCacheWarmupSummaryVO summary) {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            summary.setRedisAvailable(true);
            return true;
        } catch (Exception e) {
            markRedisFailure(summary, "redis unavailable: " + e.getMessage());
            return false;
        }
    }

    private List<Long> safeTenantIds(FrontendCacheWarmupSummaryVO summary) {
        try {
            List<Long> tenantIds = snapshotProvider.listTenantIds();
            if (CollectionUtils.isEmpty(tenantIds)) {
                return Collections.singletonList(1L);
            }
            return tenantIds;
        } catch (Exception e) {
            summary.getFailedItems().add("tenant list failed: " + e.getMessage());
            summary.setFailureCount(summary.getFailureCount() + 1);
            return Collections.singletonList(1L);
        }
    }

    private void warmupTenants(FrontendCacheWarmupSummaryVO summary, List<Long> tenantIds) {
        summary.setWarmed(true);
        for (Long tenantId : tenantIds) {
            List<FrontendCacheItem> items = loadItems(summary, tenantId);
            for (FrontendCacheItem item : items) {
                String key = buildTenantKey(tenantId, item.getContentType());
                summary.getCheckedKeys().add(key);
                try {
                    redisTemplate.opsForValue().set(
                            key,
                            JsonUtils.toJsonStr(item.getPayload()),
                            ttlSeconds(item.isEmptyResult()),
                            TimeUnit.SECONDS
                    );
                    summary.setKeyCount(summary.getKeyCount() + 1);
                } catch (RedisConnectionFailureException e) {
                    markRedisFailure(summary, "redis write failed: " + e.getMessage());
                    return;
                } catch (Exception e) {
                    summary.setFailureCount(summary.getFailureCount() + 1);
                    summary.getFailedItems().add(key + ": " + e.getMessage());
                }
            }
        }
    }

    private List<FrontendCacheItem> loadItems(FrontendCacheWarmupSummaryVO summary, Long tenantId) {
        try {
            List<FrontendCacheItem> items = snapshotProvider.loadVisibleContent(tenantId);
            if (!CollectionUtils.isEmpty(items)) {
                return items;
            }
        } catch (Exception e) {
            summary.setFailureCount(summary.getFailureCount() + 1);
            summary.getFailedItems().add("tenant " + tenantId + " load failed: " + e.getMessage());
        }
        return Collections.singletonList(new FrontendCacheItem("summary", new ArrayList<>(), true));
    }

    private FrontendCacheWarmupSummaryVO baseSummary(long start, String reason) {
        return FrontendCacheWarmupSummaryVO.builder()
                .redisAvailable(false)
                .warmed(false)
                .degraded(false)
                .reason(reason)
                .checkedAt(LocalDateTime.now())
                .elapsedMs(System.currentTimeMillis() - start)
                .checkedKeys(new ArrayList<>())
                .failedItems(new ArrayList<>())
                .build();
    }

    private FrontendCacheWarmupSummaryVO finish(FrontendCacheWarmupSummaryVO summary, long start) {
        summary.setElapsedMs(System.currentTimeMillis() - start);
        lastSummary = summary;
        if (summary.isRedisAvailable()) {
            try {
                redisTemplate.opsForValue().set(SUMMARY_KEY, JsonUtils.toJsonStr(summary), 2, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("write frontend cache warmup summary failed", e);
            }
        }
        return summary;
    }

    private void markRedisFailure(FrontendCacheWarmupSummaryVO summary, String reason) {
        summary.setRedisAvailable(false);
        summary.setDegraded(true);
        summary.setReason(reason);
        summary.setFailureCount(summary.getFailureCount() + 1);
        summary.getFailedItems().add(reason);
    }

    private static FrontendCacheWarmupSummaryVO degradedSummary(String reason) {
        return FrontendCacheWarmupSummaryVO.builder()
                .redisAvailable(false)
                .degraded(true)
                .reason(reason)
                .checkedAt(LocalDateTime.now())
                .checkedKeys(new ArrayList<>())
                .failedItems(new ArrayList<>())
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class FrontendCacheItem {
        private String contentType;
        private Object payload;
        private boolean emptyResult;
    }
}
