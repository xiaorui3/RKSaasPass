package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminK8sWorkloadActionDTO {
    private String namespace;
    private String kind;
    private String name;
    private String action;
    private Integer replicas;
    private String confirmText;
    private String reason;
}
