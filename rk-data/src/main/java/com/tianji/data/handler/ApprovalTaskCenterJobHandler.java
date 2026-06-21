package com.tianji.data.handler;

import com.tianji.data.model.vo.ApprovalTaskCenterVO;
import com.tianji.data.service.ApprovalTaskCenterService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTaskCenterJobHandler {
    public static final String HANDLER_NAME = "approvalTaskCenterWarmupJobHandler";

    private final ApprovalTaskCenterService approvalTaskCenterService;

    @XxlJob(HANDLER_NAME)
    public void warmupApprovalTaskCenter() {
        List<ApprovalTaskCenterVO> warmed = approvalTaskCenterService.warmupAllTenants();
        log.info("approval task center cache warmed, tenantCount={}", warmed.size());
    }
}
