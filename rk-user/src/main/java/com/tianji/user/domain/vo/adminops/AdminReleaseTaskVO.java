package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminReleaseTaskVO {
    private Long id;
    private Long taskId;
    private String taskType;
    private String serviceCode;
    private Long targetVersionId;
    private Long versionId;
    private String runtimeMode;
    private String status;
    private String currentStep;
    private Integer progress;
    private String logs;
    private String createTime;
    private String updateTime;
}
