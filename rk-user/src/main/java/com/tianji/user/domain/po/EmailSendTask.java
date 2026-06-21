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
@TableName("email_send_task")
public class EmailSendTask extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long senderUserId;

    private Long senderRoleId;

    private String sendType;

    private String recipientMode;

    private String subject;

    private String content;

    private Boolean html;

    private String status;

    private Integer queuedCount;

    private Integer successCount;

    private Integer failCount;

    private LocalDateTime finishedTime;
}
