package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminOpsServiceVO {
    private Long id;
    private String serviceCode;
    private String displayName;
    private String serviceType;
    private String gitSource;
    private String gitRepo;
    private String branchName;
    private String modulePath;
    private String buildMode;
    private String imageName;
    private String namespaceName;
    private String workloadType;
    private String workloadName;
    private String containerName;
    private String healthCheckPath;
    private String resourceLimits;
    private Boolean enabled;
    private Integer sortOrder;
}
