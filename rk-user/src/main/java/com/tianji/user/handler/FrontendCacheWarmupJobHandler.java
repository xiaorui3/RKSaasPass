package com.tianji.user.handler;

import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.service.IAdminOpsService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendCacheWarmupJobHandler {

    private final IAdminOpsService adminOpsService;

    @XxlJob("rkFrontendCacheWarmup")
    public void rkFrontendCacheWarmup() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(Boolean.TRUE);
        UserContext.setUser(1L);
        try {
            Map<String, Object> result = adminOpsService.warmupFrontendCache();
            String message = "successCount=" + result.getOrDefault("successCount", 0)
                    + ", failedCount=" + result.getOrDefault("failedCount", 0)
                    + ", baseUrl=" + result.getOrDefault("baseUrl", "");
            XxlJobHelper.log(message);
            XxlJobHelper.handleSuccess(message);
        } catch (Exception e) {
            log.error("frontend cache warmup job failed", e);
            XxlJobHelper.log("前台缓存预热任务失败: {}", e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
            UserContext.removeUser();
        }
    }
}
