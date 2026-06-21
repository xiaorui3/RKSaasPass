package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminK8sActionResultVO {
    private Boolean success;
    private String namespace;
    private String kind;
    private String name;
    private String action;
    private Integer replicas;
    private String message;
    private String output;
    private String executedAt;
    private Long auditId;
    private String reason;
}
