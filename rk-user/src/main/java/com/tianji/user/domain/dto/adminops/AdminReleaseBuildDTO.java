package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminReleaseBuildDTO {
    private String jobName;
    private String targetBranch;
    private String gitSource;
    private String imageMode;
    private String deployJobSuffix;
    private String remoteHost;
    private String remotePort;
    private String remoteUser;
    private String remotePassword;
    private String reason;
}
