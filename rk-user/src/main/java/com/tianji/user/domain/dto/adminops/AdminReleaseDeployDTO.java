package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminReleaseDeployDTO {
    private Long versionId;
    private String runtimeMode;
    private String targetImage;
    private String previousImage;
    private String namespace;
    private String composeProjectName;
    private String composeServiceName;
    private String composeFile;
    private String confirmText;
    private String reason;
    private Boolean dryRun;
    private Integer rolloutTimeoutSeconds;
}
