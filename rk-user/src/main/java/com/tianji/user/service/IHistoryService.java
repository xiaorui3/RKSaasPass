package com.tianji.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.search.GlobalSearchDocumentDTO;
import com.tianji.user.domain.po.HistoryEvent;

import java.util.List;
import java.util.Map;

/**
 * 历史数据服务接口
 * 基于master分支的HistoryController功能
 */
public interface IHistoryService extends IService<HistoryEvent> {

    // 获取历史时间线数据
    List<Map<String, Object>> getTimelineData();

    // 按年份获取历史事件
    List<HistoryEvent> getEventsByYear(Integer year);

    // 按类型获取历史事件
    List<HistoryEvent> getEventsByType(String eventType);

    // 获取里程碑事件
    List<HistoryEvent> getMilestoneEvents();

    // 搜索历史事件
    List<HistoryEvent> searchEvents(String keyword);

    // 添加历史事件
    boolean addEvent(HistoryEvent event);

    // 更新历史事件
    boolean updateEvent(HistoryEvent event);

    // 删除历史事件
    boolean deleteEvent(Long id);

    // 刷新缓存
    void refreshCache();

    List<GlobalSearchDocumentDTO> exportSearchDocuments();
}
