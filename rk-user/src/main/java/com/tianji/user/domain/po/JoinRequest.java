package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain = true)
@TableName("join_requests")
public class JoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    private String name;

    @TableField("student_id")
    private String studentId;

    private String major;

    private String grade;

    private String phone;

    private String email;

    @TableField("invite_token")
    private String inviteToken;

    @TableField("referral_code")
    private String referralCode;

    private String username;

    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private String password;

    @TableField(exist = false)
    private String emailCode;

    private String interest;

    private String experience;

    @TableField("auth_user_id")
    private Long authUserId;

    @TableField("source_auth_user_id")
    private Long sourceAuthUserId;

    @TableField(exist = false)
    private Long sourceTenantId;

    @TableField(exist = false)
    private String sourceRoleName;

    @TableField("form_payload_json")
    private String formPayloadJson;

    @TableField(exist = false)
    private Map<String, Object> formPayload;

    @TableField(condition = "application_time", fill = FieldFill.INSERT)
    private LocalDateTime applicationTime;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("email_sent")
    private Boolean emailSent;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
