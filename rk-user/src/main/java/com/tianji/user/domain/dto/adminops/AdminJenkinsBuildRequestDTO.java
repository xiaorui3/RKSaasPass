package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminJenkinsBuildRequestDTO {
    private String targetBranch;
    private String gitSource;
    private String services;
    private String imageMode;
    private String deployJobSuffix;
    private String remoteHost;
    private String remotePort;
    private String remoteUser;
    private String remotePassword;
}
