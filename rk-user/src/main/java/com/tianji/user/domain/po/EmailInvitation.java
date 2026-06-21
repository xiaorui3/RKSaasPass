package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("email_invitation")
public class EmailInvitation implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("sender_user_id")
    private Long senderUserId;

    @TableField("sender_role_id")
    private Long senderRoleId;

    @TableField("target_email")
    private String targetEmail;

    @TableField("invitation_type")
    private String invitationType;

    @TableField("invite_token")
    private String inviteToken;

    @TableField("referral_code")
    private String referralCode;

    @TableField("referral_code_id")
    private Long referralCodeId;

    private String status;

    private Boolean accepted;

    @TableField("accepted_time")
    private LocalDateTime acceptedTime;

    @TableField("open_time")
    private LocalDateTime openTime;

    @TableField("register_submit_time")
    private LocalDateTime registerSubmitTime;

    @TableField("register_success_time")
    private LocalDateTime registerSuccessTime;

    @TableField("join_submit_time")
    private LocalDateTime joinSubmitTime;

    @TableField("join_approved_time")
    private LocalDateTime joinApprovedTime;

    @TableField("conversion_status")
    private String conversionStatus;

    @TableField("conversion_user_id")
    private Long conversionUserId;

    @TableField("conversion_join_request_id")
    private Long conversionJoinRequestId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
