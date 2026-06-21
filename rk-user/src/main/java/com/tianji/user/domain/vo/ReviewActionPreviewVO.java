package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class ReviewActionPreviewVO {
    private String token;
    private String businessType;
    private String targetType;
    private Long targetId;
    private String action;
    private String tenantName;
    private String title;
    private String summaryHtml;
    private Boolean executable;
    private String finalState;
    private Long handledByAuthUserId;
    private String handledTime;
}
