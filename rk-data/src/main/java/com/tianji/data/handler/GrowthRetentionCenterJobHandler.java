package com.tianji.data.handler;

import com.tianji.common.utils.JsonUtils;
import com.tianji.data.model.vo.GrowthRetentionCenterVO;
import com.tianji.data.service.GrowthRetentionCenterService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrowthRetentionCenterJobHandler {
    public static final String HANDLER_NAME = "growthRetentionCenterWarmupJobHandler";

    private final GrowthRetentionCenterService growthRetentionCenterService;

    @XxlJob(HANDLER_NAME)
    public void execute() {
        List<GrowthRetentionCenterVO> warmed = growthRetentionCenterService.warmupAllTenants();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("handler", HANDLER_NAME);
        summary.put("tenantCount", warmed.size());
        summary.put("cacheKeys", warmed.stream().map(item -> item.getCache().getKey()).collect(Collectors.toList()));
        String message = JsonUtils.toJsonStr(summary);
        log.info("growth retention center warmup finished: {}", message);
        XxlJobHelper.handleSuccess(message);
    }
}
