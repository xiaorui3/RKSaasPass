package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("rk_member_achievement")
public class MemberAchievement extends BaseEntity {

    public static final int REVIEW_PENDING = 0;
    public static final int REVIEW_APPROVED = 1;
    public static final int REVIEW_REJECTED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("achievement_type")
    private Integer achievementType;

    @TableField("achievement_title")
    private String achievementTitle;

    @TableField("achievement_level")
    private Integer achievementLevel;

    private String description;

    @TableField("proof_images")
    private String proofImages;

    @TableField("award_date")
    private LocalDate awardDate;

    @TableField("award_organization")
    private String awardOrganization;

    private Integer status;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("manager_review_status")
    private Integer managerReviewStatus;

    @TableField("manager_review_comment")
    private String managerReviewComment;

    @TableField("manager_review_time")
    private LocalDateTime managerReviewTime;

    @TableField("manager_reviewer_id")
    private Long managerReviewerId;

    @TableField("teacher_review_status")
    private Integer teacherReviewStatus;

    @TableField("teacher_review_comment")
    private String teacherReviewComment;

    @TableField("teacher_review_time")
    private LocalDateTime teacherReviewTime;

    @TableField("teacher_reviewer_id")
    private Long teacherReviewerId;

    @TableField(exist = false)
    private String userName;
}
