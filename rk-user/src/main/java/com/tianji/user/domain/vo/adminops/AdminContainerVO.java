package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminContainerVO {
    private String name;
    private String namespace;
    private String podName;
    private String nodeName;
    private String podIp;
    private String status;
    private Integer restartCount;
    private String containerName;
    private String image;
    private String ports;
}
