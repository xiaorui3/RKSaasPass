package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminJenkinsBuildStatusVO {
    private String jobName;
    private String queueId;
    private Integer buildNumber;
    private String buildUrl;
    private String status;
    private Boolean building;
    private String result;
    private Long durationMillis;
    private Long estimatedDurationMillis;
    private String targetBranch;
    private String gitSource;
    private String gitSourceLabel;
    private String services;
    private String imageMode;
    private String imageModeLabel;
    private String logTail;
    private String message;
    private String preflightSummary;
}
