package com.tianji.data.handler;

import com.tianji.common.utils.JsonUtils;
import com.tianji.data.model.vo.ParticipationAnalysisCenterVO;
import com.tianji.data.service.ParticipationAnalysisCenterService;
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
public class ParticipationAnalysisCenterJobHandler {
    public static final String HANDLER_NAME = "participationAnalysisCenterWarmupJobHandler";

    private final ParticipationAnalysisCenterService participationAnalysisCenterService;

    @XxlJob(HANDLER_NAME)
    public void execute() {
        List<ParticipationAnalysisCenterVO> warmed = participationAnalysisCenterService.warmupAllTenants();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("handler", HANDLER_NAME);
        summary.put("tenantCount", warmed.size());
        summary.put("cacheKeys", warmed.stream().map(item -> item.getCache().getKey()).collect(Collectors.toList()));
        String message = JsonUtils.toJsonStr(summary);
        log.info("participation analysis center warmup finished: {}", message);
        XxlJobHelper.handleSuccess(message);
    }
}
