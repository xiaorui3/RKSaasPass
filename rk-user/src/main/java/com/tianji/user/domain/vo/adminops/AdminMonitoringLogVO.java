package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminMonitoringLogVO {
    private String namespace;
    private String podName;
    private String containerName;
    private Integer tailLines;
    private String logs;
    private String message;
}
