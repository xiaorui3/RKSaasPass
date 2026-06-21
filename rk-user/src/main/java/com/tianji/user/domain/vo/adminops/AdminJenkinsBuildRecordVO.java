package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminJenkinsBuildRecordVO {
    private Long id;
    private String jobName;
    private Integer buildNumber;
    private String buildUrl;
    private String queueId;
    private String gitSource;
    private String gitRepo;
    private String branchName;
    private String commitId;
    private String imageMode;
    private String services;
    private String status;
    private String stage;
    private String result;
    private String failureReason;
    private Long triggerUserId;
    private String triggerUserName;
    private String triggerTime;
    private String startTime;
    private String finishTime;
    private Long durationMs;
    private String logTail;
    private String preflightSummary;
}
