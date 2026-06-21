package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sClusterMaintenanceDTO {
    private List<String> actions;
    private List<String> namespaces;
    private List<String> statuses;
    private Boolean dryRun;
    private Boolean cleanupNodeDisk;
    private Boolean cleanupAbnormalPods;
    private Boolean includeControllerManaged;
    private String confirmText;
    private String reason;
}
