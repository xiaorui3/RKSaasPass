package com.tianji.data.handler;

import com.tianji.common.utils.JsonUtils;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.FrontendCacheWarmupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendCacheWarmupJobHandler {

    public static final String HANDLER_NAME = "frontendCacheWarmupJobHandler";

    private final FrontendCacheWarmupService warmupService;

    @XxlJob(HANDLER_NAME)
    public void execute() {
        FrontendCacheWarmupSummaryVO summary = warmupService.checkAndWarmup();
        String message = JsonUtils.toJsonStr(summary);
        log.info("frontend cache warmup finished: {}", message);
        if (summary.isDegraded()) {
            XxlJobHelper.handleFail(message);
        } else {
            XxlJobHelper.handleSuccess(message);
        }
    }
}
