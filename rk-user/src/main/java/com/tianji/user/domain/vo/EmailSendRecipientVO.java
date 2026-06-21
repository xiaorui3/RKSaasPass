package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class EmailSendRecipientVO {
    private Long id;
    private Long taskId;
    private String recipientEmail;
    private String recipientName;
    private String sourceType;
    private String sendStatus;
    private String errorMessage;
    private String queuedTime;
    private String sentTime;
}
