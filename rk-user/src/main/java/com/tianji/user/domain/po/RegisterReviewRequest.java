package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("register_review_request")
public class RegisterReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("auth_user_id")
    private Long authUserId;

    private String username;

    private String email;

    private String name;

    @TableField("referral_code")
    private String referralCode;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("reviewer_auth_user_id")
    private Long reviewerAuthUserId;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("form_payload_json")
    private String formPayloadJson;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
