package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("email_send_recipient")
public class EmailSendRecipient extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String recipientEmail;

    private String recipientName;

    private Long recipientUserId;

    private String sourceType;

    private Long sourceRefId;

    private String sendStatus;

    private String errorMessage;

    private String providerMessageId;

    private LocalDateTime queuedTime;

    private LocalDateTime sentTime;

    private LocalDateTime lastAttemptTime;

    private Integer retryCount;
}
