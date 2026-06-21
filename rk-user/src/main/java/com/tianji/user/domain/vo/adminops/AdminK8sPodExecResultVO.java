package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminK8sPodExecResultVO {
    private String namespace;
    private String podName;
    private String containerName;
    private String command;
    private Boolean success;
    private Integer exitCode;
    private String output;
    private String message;
    private Long durationMillis;
    private Long auditId;
    private String executedAt;
}
