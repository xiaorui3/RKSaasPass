package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminK8sCleanupRequestDTO {
    private String nodeName;
    private String nodeIp;
    private List<String> actions;
    private Boolean dryRun;
    private String confirmText;
    private String reason;
}
