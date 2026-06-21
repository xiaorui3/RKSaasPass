package com.tianji.content.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作品表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("works")
public class Work implements Serializable {

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
     * 描述
     */
    private String description;

    /**
     * 内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 封面图片
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 演示视频URL
     */
    @TableField("demo_video")
    private String demoVideo;

    /**
     * 项目链接（JSON格式）
     */
    @TableField("project_links")
    private String projectLinks;

    /**
     * 技术栈（JSON格式）
     */
    @TableField("technologies")
    private String technologies;

    /**
     * 是否精选
     */
    @TableField("is_featured")
    private Boolean isFeatured;

    /**
     * 展示顺序
     */
    @TableField("display_order")
    private Integer displayOrder;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞次数
     */
    @TableField("like_count")
    private Integer likeCount;

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
     * 作者（JSON格式）
     */
    private String authors;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
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
