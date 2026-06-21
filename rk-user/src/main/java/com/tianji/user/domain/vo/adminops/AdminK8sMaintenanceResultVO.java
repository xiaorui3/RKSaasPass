package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminK8sMaintenanceResultVO {
    private Boolean success;
    private Boolean dryRun;
    private String message;
    private String executedAt;
    private String reason;
    private Integer abnormalPodCount;
    private Integer abnormalPodDeletedCount;
    private List<AdminK8sAbnormalPodVO> abnormalPods;
    private List<AdminK8sCleanupResultVO> nodeResults;
    private Map<String, Object> schedule;
    private Long auditId;
    private String output;
}
