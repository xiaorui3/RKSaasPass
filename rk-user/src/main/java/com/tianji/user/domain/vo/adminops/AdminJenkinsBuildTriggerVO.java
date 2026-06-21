package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminJenkinsBuildTriggerVO {
    private Long recordId;
    private Boolean accepted;
    private Integer statusCode;
    private String jobName;
    private String queueId;
    private String queueUrl;
    private String targetBranch;
    private String gitSource;
    private String gitSourceLabel;
    private String services;
    private String imageMode;
    private String imageModeLabel;
    private String message;
    private String preflightSummary;
}
