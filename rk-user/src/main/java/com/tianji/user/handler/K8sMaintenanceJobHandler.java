package com.tianji.user.handler;

import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.dto.adminops.AdminK8sClusterMaintenanceDTO;
import com.tianji.user.domain.vo.adminops.AdminK8sMaintenanceResultVO;
import com.tianji.user.service.IAdminOpsService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class K8sMaintenanceJobHandler {

    private final IAdminOpsService adminOpsService;

    @XxlJob("rkK8sMaintenanceJobHandler")
    public void rkK8sMaintenanceJobHandler() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(Boolean.TRUE);
        UserContext.setUser(1L);
        try {
            AdminK8sClusterMaintenanceDTO dto = new AdminK8sClusterMaintenanceDTO();
            dto.setActions(List.of("IMAGE_PRUNE", "LOG_CLEAN", "NODE_CACHE_CLEAN"));
            dto.setCleanupNodeDisk(Boolean.TRUE);
            dto.setCleanupAbnormalPods(Boolean.TRUE);
            dto.setIncludeControllerManaged(Boolean.FALSE);
            dto.setDryRun(Boolean.FALSE);
            dto.setConfirmText("CLEAN_CLUSTER");
            dto.setReason("xxl-job half-month k8s maintenance");
            AdminK8sMaintenanceResultVO result = adminOpsService.runK8sClusterMaintenance(dto);
            String message = "success=" + result.getSuccess()
                    + ", abnormalPods=" + result.getAbnormalPodCount()
                    + ", deletedCandidates=" + result.getAbnormalPodDeletedCount()
                    + ", nodeResults=" + (result.getNodeResults() == null ? 0 : result.getNodeResults().size());
            if (Boolean.TRUE.equals(result.getSuccess())) {
                XxlJobHelper.handleSuccess(message);
            } else {
                XxlJobHelper.handleFail(message + "\n" + result.getOutput());
            }
        } catch (Exception e) {
            log.error("rk k8s maintenance job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
            UserContext.removeUser();
        }
    }
}
