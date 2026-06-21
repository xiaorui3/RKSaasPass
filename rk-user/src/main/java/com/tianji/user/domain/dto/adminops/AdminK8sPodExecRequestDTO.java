package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminK8sPodExecRequestDTO {
    private String namespace;
    private String podName;
    private String containerName;
    private String command;
    private Integer timeoutSeconds;
    private String reason;
}
