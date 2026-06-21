package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sAbnormalPodCleanupDTO {
    private List<String> namespaces;
    private List<String> statuses;
    private Boolean includeControllerManaged;
    private Boolean dryRun;
    private String confirmText;
    private String reason;
}
