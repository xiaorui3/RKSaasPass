package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminOpsAuditLogVO {
    private Long id;
    private String operationType;
    private String targetNamespace;
    private String targetKind;
    private String targetName;
    private String action;
    private String status;
    private String reason;
    private String requestPayload;
    private String resultOutput;
    private Long operatorUserId;
    private String operatorUserName;
    private String createTime;
}
