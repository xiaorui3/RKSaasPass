package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminReleaseDeploymentContext {
    private String serviceCode;
    private String runtimeMode;
    private String targetImage;
    private String previousImage;
    private String namespace;
    private String workloadType;
    private String workloadResourceType;
    private String workloadName;
    private String containerName;
    private Boolean dryRun;
    private Integer rolloutTimeoutSeconds;
    private String composeProjectName;
    private String composeServiceName;
    private String composeFile;
}
