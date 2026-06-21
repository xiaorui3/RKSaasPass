package com.tianji.data.service;

import com.tianji.data.model.vo.ContentPublishCalendarVO;

import java.util.List;

public interface ContentPublishCalendarService {
    ContentPublishCalendarVO getContentPublishCalendar(Long tenantId, String timeRange, String contentType, String status);

    List<ContentPublishCalendarVO> warmupAllTenants();
}
