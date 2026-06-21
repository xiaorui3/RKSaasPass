package com.tianji.data.handler;

import com.tianji.data.model.vo.SecurityAuditCenterVO;
import com.tianji.data.service.SecurityAuditCenterService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditCenterJobHandler {
    public static final String HANDLER_NAME = "securityAuditCenterWarmupJobHandler";

    private final SecurityAuditCenterService securityAuditCenterService;

    @XxlJob(HANDLER_NAME)
    public void warmupSecurityAuditCenter() {
        List<SecurityAuditCenterVO> warmed = securityAuditCenterService.warmupAllTenants();
        log.info("security audit center cache warmed, tenantCount={}", warmed.size());
    }
}
