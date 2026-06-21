package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sCleanupResultVO {
    private String nodeName;
    private String nodeIp;
    private List<String> actions;
    private Boolean dryRun;
    private Boolean success;
    private String message;
    private String command;
    private String output;
    private String executedAt;
    private Long auditId;
    private String reason;
}
