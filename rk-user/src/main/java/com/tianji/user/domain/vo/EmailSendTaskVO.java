package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class EmailSendTaskVO {
    private Long id;
    private Long tenantId;
    private String sendType;
    private String recipientMode;
    private String subject;
    private String content;
    private Boolean html;
    private String status;
    private Integer queuedCount;
    private Integer successCount;
    private Integer failCount;
    private String createTime;
    private String finishedTime;
}
