package com.tianji.message.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("rk_notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int REVIEW_PENDING = 0;
    public static final int REVIEW_APPROVED = 1;
    public static final int REVIEW_REJECTED = 2;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 通知类型: 1-系统公告 2-活动通知 3-比赛通知 4-其他
     */
    @TableField("notice_type")
    private Integer noticeType;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 附件URL
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 通知等级
     */
    @TableField("notice_level")
    private String noticeLevel;

    /**
     * 目标类型
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标ID列表
     */
    @TableField("target_ids")
    private String targetIds;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /**
     * 是否置顶
     */
    @TableField("is_top")
    private Integer isTop;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 是否已发布
     */
    @TableField("is_published")
    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    private Boolean isPublished;

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

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    /**
     * 更新者ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;

    /**
     * 删除标记：0-未删除 1-已删除（企业级多租户标准）
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
