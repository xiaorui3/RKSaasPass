package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.common.utils.SearchIndexSyncUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.HistoryEvent;
import com.tianji.user.mapper.HistoryEventMapper;
import com.tianji.user.service.IHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史数据服务实现类
 * 基于master分支的HistoryCacheService功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl extends ServiceImpl<HistoryEventMapper, HistoryEvent>
        implements IHistoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchClient searchClient;

    private static final String CACHE_KEY = "history:timeline";
    private static final String SEARCH_ENTITY_TYPE_HISTORY = "HISTORY";

    /**
     * 获取历史时间线数据
     * 基于master分支的实现，使用Redis缓存
     */
    @Override
    public List<Map<String, Object>> getTimelineData() {
        String cacheKey = currentCacheKey();
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedData = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取到{}个历史事件", cachedData.size());
            return cachedData;
        }

        // 缓存未命中，查询数据库
        List<HistoryEvent> events = getAllActiveEvents();
        List<Map<String, Object>> timelineData = convertToTimelineFormat(events);

        // 存入缓存（30分钟过期）
        redisTemplate.opsForValue().set(cacheKey, timelineData, 30, java.util.concurrent.TimeUnit.MINUTES);

        log.info("从数据库获取到{}个历史事件", timelineData.size());
        return timelineData;
    }

    /**
     * 按年份获取历史事件
     */
    @Override
    public List<HistoryEvent> getEventsByYear(Integer year) {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getTenantId, resolveTenantId())
                .eq(HistoryEvent::getYear, year)
                .eq(HistoryEvent::getIsActive, true)
                .eq(HistoryEvent::getIsDeleted, 0)
                .orderByAsc(HistoryEvent::getEventDate);
        return list(queryWrapper);
    }

    private boolean isPublicHistory(HistoryEvent event) {
        return event != null
                && Boolean.TRUE.equals(event.getIsActive())
                && Integer.valueOf(0).equals(event.getIsDeleted());
    }

    private void syncHistorySearchIndex(Long id) {
        if (id == null) {
            return;
        }
        SearchIndexSyncUtils.runAfterCommit(() -> {
            HistoryEvent event = null;
            try {
                event = getById(id);
                if (event == null || !isPublicHistory(event)) {
                    searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_HISTORY, id,
                            event == null ? TenantContext.getTenantId() : event.getTenantId());
                    return;
                }
                searchClient.upsertGlobalDocument(buildHistorySearchDocument(event));
            } catch (Exception e) {
                log.warn("sync history global search index failed, historyId={}, tenantId={}, reason={}",
                        id, event == null ? null : event.getTenantId(), e.getMessage());
            }
        });
    }

    private void deleteHistorySearchIndex(Long id, Long tenantId) {
        if (id == null) {
            return;
        }
        Long resolvedTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        SearchIndexSyncUtils.runAfterCommit(() -> {
            try {
                searchClient.deleteGlobalDocument(SEARCH_ENTITY_TYPE_HISTORY, id, resolvedTenantId);
            } catch (Exception e) {
                log.warn("delete history global search index failed, historyId={}, tenantId={}, reason={}",
                        id, resolvedTenantId, e.getMessage());
            }
        });
    }

    @Override
    public List<GlobalSearchDocumentDTO> exportSearchDocuments() {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getIsActive, true)
                .eq(HistoryEvent::getIsDeleted, 0)
                .orderByDesc(HistoryEvent::getEventDate)
                .orderByDesc(HistoryEvent::getUpdateTime);
        return list(queryWrapper).stream()
                .filter(this::isPublicHistory)
                .map(this::buildHistorySearchDocument)
                .collect(Collectors.toList());
    }

    private GlobalSearchDocumentDTO buildHistorySearchDocument(HistoryEvent event) {
        return new GlobalSearchDocumentDTO()
                .setEntityType(SEARCH_ENTITY_TYPE_HISTORY)
                .setEntityId(event.getId())
                .setTenantId(event.getTenantId())
                .setTitle(event.getTitle())
                .setSummary(event.getDescription())
                .setContent(joinSearchText(event.getRelatedMembers(), event.getExtraInfo(), event.getEventType()))
                .setTags(event.getTags())
                .setRoute("/history")
                .setCoverUrl(event.getCoverImageUrl())
                .setUpdatedAt(formatUpdatedAt(event.getUpdateTime(), event.getCreateTime()))
                .setVisible(isPublicHistory(event));
    }

    private String joinSearchText(String... values) {
        if (values == null) {
            return null;
        }
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private String formatUpdatedAt(LocalDateTime... times) {
        if (times == null) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time != null) {
                return time.toString();
            }
        }
        return null;
    }

    /**
     * 按类型获取历史事件
     */
    @Override
    public List<HistoryEvent> getEventsByType(String eventType) {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getTenantId, resolveTenantId())
                .eq(HistoryEvent::getEventType, eventType)
                .eq(HistoryEvent::getIsActive, true)
                .eq(HistoryEvent::getIsDeleted, 0)
                .orderByDesc(HistoryEvent::getImportanceLevel)
                .orderByAsc(HistoryEvent::getEventDate);
        return list(queryWrapper);
    }

    /**
     * 获取里程碑事件
     */
    @Override
    public List<HistoryEvent> getMilestoneEvents() {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getTenantId, resolveTenantId())
                .eq(HistoryEvent::getIsMilestone, true)
                .eq(HistoryEvent::getIsActive, true)
                .eq(HistoryEvent::getIsDeleted, 0)
                .orderByDesc(HistoryEvent::getImportanceLevel)
                .orderByAsc(HistoryEvent::getEventDate);
        return list(queryWrapper);
    }

    /**
     * 搜索历史事件
     */
    @Override
    public List<HistoryEvent> searchEvents(String keyword) {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getTenantId, resolveTenantId())
        .and(wrapper -> wrapper
                .like(HistoryEvent::getTitle, keyword)
                .or()
                .like(HistoryEvent::getDescription, keyword)
        )
        .eq(HistoryEvent::getIsActive, true)
        .eq(HistoryEvent::getIsDeleted, 0)
        .orderByDesc(HistoryEvent::getImportanceLevel)
        .orderByAsc(HistoryEvent::getEventDate);
        return list(queryWrapper);
    }

    /**
     * 添加历史事件
     */
    @Override
    public boolean addEvent(HistoryEvent event) {
        if (event.getIsActive() == null) {
            event.setIsActive(true);
        }
        if (event.getIsDeleted() == null) {
            event.setIsDeleted(0);
        }
        if (event.getCreateTime() == null) {
            event.setCreateTime(LocalDateTime.now());
        }
        if (event.getUpdateTime() == null) {
            event.setUpdateTime(LocalDateTime.now());
        }

        boolean result = save(event);
        if (result) {
            // 清除缓存
            refreshCache();
            syncHistorySearchIndex(event.getId());
            log.info("✅ 添加历史事件成功，已清除缓存");
        }
        return result;
    }

    /**
     * 更新历史事件
     */
    @Override
    public boolean updateEvent(HistoryEvent event) {
        if (event.getUpdateTime() == null) {
            event.setUpdateTime(LocalDateTime.now());
        }
        boolean result = updateById(event);
        if (result) {
            // 清除缓存
            refreshCache();
            syncHistorySearchIndex(event.getId());
            log.info("✅ 更新历史事件成功，已清除缓存");
        }
        return result;
    }

    /**
     * 删除历史事件
     */
    @Override
    public boolean deleteEvent(Long id) {
        HistoryEvent existing = getById(id);
        Long tenantId = existing == null ? null : existing.getTenantId();
        boolean result = removeById(id);
        if (result) {
            // 清除缓存
            refreshCache();
            deleteHistorySearchIndex(id, tenantId);
            log.info("✅ 删除历史事件成功，已清除缓存");
        }
        return result;
    }

    /**
     * 刷新缓存
     * 基于master分支的refreshCache功能
     */
    @Override
    public void refreshCache() {
        redisTemplate.delete(CACHE_KEY);
        Set<String> keys = redisTemplate.keys(CACHE_KEY + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("🗑️ 历史数据缓存已清除");
    }

    /**
     * 获取所有活跃的历史事件
     */
    private List<HistoryEvent> getAllActiveEvents() {
        LambdaQueryWrapper<HistoryEvent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HistoryEvent::getTenantId, resolveTenantId())
                .eq(HistoryEvent::getIsActive, true)
                .eq(HistoryEvent::getIsDeleted, 0)
                .orderByDesc(HistoryEvent::getImportanceLevel)
                .orderByAsc(HistoryEvent::getEventDate);
        return list(queryWrapper);
    }

    /**
     * 转换为时间线格式
     * 基于master分支的返回格式
     */
    private List<Map<String, Object>> convertToTimelineFormat(List<HistoryEvent> events) {
        return events.stream().map(event -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", event.getId());
            map.put("date", event.getEventDate());
            map.put("eventDate", event.getEventDate());
            map.put("year", event.getYear());
            map.put("title", event.getTitle());
            map.put("description", event.getDescription());
            map.put("type", event.getEventType());
            map.put("eventType", event.getEventType());
            map.put("importance", event.getImportanceLevel());
            map.put("isMilestone", event.getIsMilestone());
            map.put("coverImage", event.getCoverImageUrl());
            return map;
        }).collect(Collectors.toList());
    }

    private String currentCacheKey() {
        return CACHE_KEY + ":" + resolveTenantId();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
