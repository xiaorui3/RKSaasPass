package com.tianji.data.controller;

import com.tianji.common.domain.R;
import com.tianji.data.model.vo.ContentPublishCalendarVO;
import com.tianji.data.service.ContentPublishCalendarService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/content-publish-calendar")
@Api(tags = "内容发布日历")
public class ContentPublishCalendarController {

    @Autowired
    private ContentPublishCalendarService contentPublishCalendarService;

    @GetMapping
    @ApiOperation("获取内容发布日历聚合数据")
    public R<ContentPublishCalendarVO> getContentPublishCalendar(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "status", required = false) String status) {
        return R.ok(contentPublishCalendarService.getContentPublishCalendar(tenantId, timeRange, contentType, status));
    }
}
