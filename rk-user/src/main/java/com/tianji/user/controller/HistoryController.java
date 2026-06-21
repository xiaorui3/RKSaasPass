package com.tianji.user.controller;

import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.user.domain.po.HistoryEvent;
import com.tianji.user.service.IHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 历史数据控制器
 * 完全基于master分支的HistoryController实现
 */
@Slf4j
@Api(tags = "历史数据接口")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final IHistoryService historyService;

    /**
     * 获取历史时间线数据
     * 完全对应master分支的接口
     */
    @ApiOperation("获取历史时间线数据")
    @GetMapping("/timeline")
    public R<List<Map<String, Object>>> getTimeline() {
        try {
            List<Map<String, Object>> events = historyService.getTimelineData();
            log.info("从Redis缓存获取到{}个历史事件", events.size());
            return R.ok(events);
        } catch (Exception e) {
            log.error("获取历史时间线数据失败", e);
            return R.error("获取历史数据失败：" + e.getMessage());
        }
    }

    /**
     * 刷新历史数据缓存
     * 完全对应master分支的接口
     */
    @ApiOperation("刷新历史数据缓存")
    @PostMapping("/refresh")
    public R<String> refreshCache() {
        try {
            historyService.refreshCache();
            return R.ok("历史数据缓存刷新成功");
        } catch (Exception e) {
            log.error("刷新历史数据缓存失败", e);
            return R.error("刷新缓存失败：" + e.getMessage());
        }
    }

    /**
     * 获取缓存状态
     * 完全对应master分支的接口
     */
    @ApiOperation("获取缓存状态")
    @GetMapping("/cache-status")
    public R<String> getCacheStatus() {
        try {
            List<Map<String, Object>> events = historyService.getTimelineData();
            return R.ok(String.format("缓存中包含%d个历史事件", events.size()));
        } catch (Exception e) {
            return R.error("获取缓存状态失败：" + e.getMessage());
        }
    }

    /**
     * 按年份获取历史事件
     */
    @ApiOperation("按年份获取历史事件")
    @GetMapping("/year/{year}")
    public R<List<Map<String, Object>>> getEventsByYear(@PathVariable Integer year) {
        try {
            return R.ok(historyService.getEventsByYear(year)
                    .stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return R.error("获取历史事件失败：" + e.getMessage());
        }
    }

    /**
     * 按类型获取历史事件
     */
    @ApiOperation("按类型获取历史事件")
    @GetMapping("/type/{type}")
    public R<List<Map<String, Object>>> getEventsByType(@PathVariable String type) {
        try {
            return R.ok(historyService.getEventsByType(type)
                    .stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("按类型获取历史事件失败", e);
            return R.error("获取历史事件失败：" + e.getMessage());
        }
    }

    /**
     * 获取里程碑事件（重要历史事件）
     */
    @ApiOperation("获取里程碑事件")
    @GetMapping("/milestones")
    public R<List<Map<String, Object>>> getMilestoneEvents() {
        try {
            return R.ok(historyService.getMilestoneEvents()
                    .stream()
                    .map(event -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", event.getId());
                        map.put("date", event.getEventDate());
                        map.put("title", event.getTitle());
                        map.put("description", event.getDescription());
                        map.put("importance", event.getImportanceLevel());
                        map.put("coverImage", event.getCoverImageUrl());
                        return map;
                    })
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return R.error("获取里程碑事件失败：" + e.getMessage());
        }
    }

    /**
     * 获取重要历史事件（前端接口名称）
     */
    @ApiOperation("获取重要历史事件")
    @GetMapping("/important")
    public R<List<Map<String, Object>>> getImportantEvents() {
        try {
            return R.ok(historyService.getMilestoneEvents()
                    .stream()
                    .map(event -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", event.getId());
                        map.put("date", event.getEventDate());
                        map.put("title", event.getTitle());
                        map.put("description", event.getDescription());
                        map.put("type", event.getEventType());
                        map.put("importance", event.getImportanceLevel());
                        map.put("coverImage", event.getCoverImageUrl());
                        map.put("isMilestone", event.getIsMilestone());
                        return map;
                    })
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("获取重要历史事件失败", e);
            return R.error("获取重要历史事件失败：" + e.getMessage());
        }
    }

    /**
     * 获取历史事件详情
     */
    @ApiOperation("获取历史事件详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getEventDetail(@PathVariable Long id) {
        try {
            HistoryEvent event = historyService.getById(id);
            if (event == null || event.getIsDeleted() == 1) {
                return R.error("历史事件不存在");
            }
            return R.ok(convertToDetailMap(event));
        } catch (Exception e) {
            log.error("获取历史事件详情失败", e);
            return R.error("获取历史事件详情失败：" + e.getMessage());
        }
    }

    /**
     * 搜索历史事件
     */
    @ApiOperation("搜索历史事件")
    @GetMapping("/search")
    public R<List<Map<String, Object>>> searchEvents(@RequestParam String keyword) {
        try {
            return R.ok(historyService.searchEvents(keyword)
                    .stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("搜索历史事件失败", e);
            return R.error("搜索历史事件失败：" + e.getMessage());
        }
    }

    /**
     * 获取统计数据
     */
    @ApiOperation("获取统计数据")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getHistoryStatistics() {
        try {
            List<Map<String, Object>> allEvents = historyService.getTimelineData();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", allEvents.size());
            statistics.put("milestoneCount", allEvents.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("isMilestone")))
                    .count());
            
            // 按类型统计
            Map<String, Long> byType = allEvents.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.get("type") != null ? e.get("type").toString() : "未分类",
                            Collectors.counting()
                    ));
            statistics.put("byType", byType);
            
            // 按年份统计
            Map<Integer, Long> byYear = allEvents.stream()
                    .filter(e -> e.get("year") != null)
                    .collect(Collectors.groupingBy(
                            e -> (Integer) e.get("year"),
                            Collectors.counting()
                    ));
            statistics.put("byYear", byYear);
            
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return R.error("获取统计数据失败：" + e.getMessage());
        }
    }

    @ApiOperation("后台分页查询历史事件")
    @GetMapping("/admin/page")
    public R<Map<String, Object>> adminPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long tenantId = resolveTenantId();
            LambdaQueryWrapper<HistoryEvent> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HistoryEvent::getTenantId, tenantId)
                    .eq(HistoryEvent::getIsDeleted, 0);
            if (year != null) {
                wrapper.eq(HistoryEvent::getYear, year);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String value = keyword.trim();
                wrapper.and(item -> item.like(HistoryEvent::getTitle, value)
                        .or()
                        .like(HistoryEvent::getDescription, value));
            }
            wrapper.orderByAsc(HistoryEvent::getSortOrder)
                    .orderByDesc(HistoryEvent::getEventDate)
                    .orderByDesc(HistoryEvent::getId);
            List<HistoryEvent> all = historyService.list(wrapper);
            int safeCurrent = Math.max(current == null ? 1 : current, 1);
            int safeSize = Math.min(Math.max(size == null ? 10 : size, 1), 100);
            int from = Math.min((safeCurrent - 1) * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            Map<String, Object> result = new HashMap<>();
            result.put("records", all.subList(from, to).stream().map(this::convertToDetailMap).collect(Collectors.toList()));
            result.put("total", all.size());
            result.put("current", safeCurrent);
            result.put("size", safeSize);
            return R.ok(result);
        } catch (Exception e) {
            log.error("后台分页查询历史事件失败", e);
            return R.error("查询历史事件失败：" + e.getMessage());
        }
    }

    @ApiOperation("后台保存历史事件")
    @PostMapping("/admin/save")
    public R<HistoryEvent> adminSave(@RequestBody HistoryEvent event) {
        try {
            Long tenantId = resolveTenantId();
            event.setTenantId(tenantId);
            if (event.getYear() == null && event.getEventDate() != null) {
                event.setYear(event.getEventDate().getYear());
            }
            if (event.getImportanceLevel() == null) {
                event.setImportanceLevel(1);
            }
            if (event.getSortOrder() == null) {
                event.setSortOrder(0);
            }
            boolean saved = event.getId() == null ? historyService.addEvent(event) : historyService.updateEvent(event);
            return saved ? R.ok(historyService.getById(event.getId())) : R.error("保存历史事件失败");
        } catch (Exception e) {
            log.error("保存历史事件失败", e);
            return R.error("保存历史事件失败：" + e.getMessage());
        }
    }

    @ApiOperation("后台删除历史事件")
    @PostMapping("/admin/{id}/delete")
    public R<Boolean> adminDelete(@PathVariable Long id) {
        try {
            HistoryEvent event = historyService.getById(id);
            if (event == null || event.getIsDeleted() == 1 || !resolveTenantId().equals(event.getTenantId())) {
                return R.error("历史事件不存在");
            }
            return historyService.deleteEvent(id) ? R.ok(true) : R.error("删除历史事件失败");
        } catch (Exception e) {
            log.error("删除历史事件失败", e);
            return R.error("删除历史事件失败：" + e.getMessage());
        }
    }

    /**
     * 转换为Map格式
     */
    private Map<String, Object> convertToMap(HistoryEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("date", event.getEventDate());
        map.put("year", event.getYear());
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("type", event.getEventType());
        map.put("importance", event.getImportanceLevel());
        map.put("isMilestone", event.getIsMilestone());
        map.put("isActive", event.getIsActive());
        map.put("sortOrder", event.getSortOrder());
        map.put("coverImage", event.getCoverImageUrl());
        return map;
    }

    /**
     * 转换为详情Map格式
     */
    private Map<String, Object> convertToDetailMap(HistoryEvent event) {
        Map<String, Object> map = convertToMap(event);
        map.put("relatedMembers", event.getRelatedMembers());
        map.put("tags", event.getTags());
        map.put("extraInfo", event.getExtraInfo());
        map.put("createTime", event.getCreateTime());
        map.put("updateTime", event.getUpdateTime());
        return map;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
