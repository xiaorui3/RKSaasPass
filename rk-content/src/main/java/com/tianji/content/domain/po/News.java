package com.tianji.content.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新闻表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("news")
public class News implements Serializable {

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
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 视频URL（支持视频上传）
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 附件URL
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 作者
     */
    private String author;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（多个用逗号分隔）
     */
    private String tags;

    /**
     * 是否发布：0-草稿，1-发布
     */
    @TableField("is_published")
    private Integer isPublished;

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
     * 审核状态：0-草稿 1-待审核 2-已通过 3-已拒绝
     */
    @TableField("approval_status")
    private Integer approvalStatus;

    /**
     * 审核人
     */
    @TableField("approver")
    private String approver;

    /**
     * 审核时间
     */
    @TableField("approval_time")
    private LocalDateTime approvalTime;

    /**
     * 拒绝原因
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 是否置顶：0-否，1-是
     */
    @TableField("is_featured")
    private Integer isFeatured;

    /**
     * 是否允许跨租户公开展示：0-否，1-是
     */
    @TableField("is_cross_tenant")
    private Integer isCrossTenant;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建者
     */
    @TableField("created_by")
    private String createdBy;

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
     * 删除标记：0-未删除 1-已删除（企业级多租户标准）
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
