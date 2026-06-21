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
@TableName("email_invitation_event")
public class EmailInvitationEvent extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long invitationId;

    private String inviteToken;

    private String referralCode;

    private String eventType;

    private LocalDateTime eventTime;

    private String actorEmail;

    private Long actorUserId;

    private Long joinRequestId;

    private String remark;
}
