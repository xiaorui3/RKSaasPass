package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminK8sAbnormalPodVO {
    private String namespace;
    private String name;
    private String status;
    private String phase;
    private String reason;
    private String message;
    private String nodeName;
    private String podIp;
    private String createdAt;
    private String age;
    private Integer restartCount;
    private String ownerKind;
    private String ownerName;
    private Boolean controllerManaged;
    private Boolean deletable;
    private String suggestion;
}
