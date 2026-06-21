package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动实体类（支持多租户）
 * 对应数据库表：rk_activity.rk_activity
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("rk_activity")
public class Activity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称
     */
    @TableField("activity_name")
    private String activityName;

    /**
     * 活动编码
     */
    @TableField("activity_code")
    private String activityCode;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 封面图片
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 附件URL
     */
    @TableField("attachment_url")
    private String attachmentUrl;

    /**
     * 活动类型
     */
    @TableField("activity_type")
    private Integer activityType;

    /**
     * 组织者
     */
    private String organizer;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 在线链接
     */
    @TableField("online_url")
    private String onlineUrl;

    /**
     * 最大参与人数
     */
    @TableField("max_participants")
    private Integer maxParticipants;

    /**
     * 当前参与人数
     */
    @TableField("current_participants")
    private Integer currentParticipants;

    /**
     * 活动状态：1-未开始 2-报名中 3-进行中 4-已结束
     */
    @TableField("activity_status")
    private Integer activityStatus;

    /**
     * 活动开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 报名开始时间
     */
    @TableField("registration_start_time")
    private LocalDateTime registrationStartTime;

    /**
     * 报名结束时间
     */
    @TableField("registration_end_time")
    private LocalDateTime registrationEndTime;

    /**
     * 活动内容
     */
    private String content;

    /**
     * 参与要求
     */
    private String requirements;

    /**
     * 活动积分
     */
    private Integer points;

    /**
     * 是否置顶
     */
    @TableField("is_top")
    private Integer isTop;

    /**
     * 是否热门
     */
    @TableField("is_hot")
    private Integer isHot;

    /**
     * 是否允许跨租户展示/报名：0-否，1-是
     */
    @TableField("is_cross_tenant")
    private Integer isCrossTenant;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 负责人审核状态：0-待审核 1-已通过 2-已拒绝
     */
    @TableField("manager_review_status")
    private Integer managerReviewStatus;

    /**
     * 负责人审核意见
     */
    @TableField("manager_review_comment")
    private String managerReviewComment;

    /**
     * 负责人审核时间
     */
    @TableField("manager_review_time")
    private LocalDateTime managerReviewTime;

    /**
     * 负责人审核人
     */
    @TableField("manager_reviewer_id")
    private Long managerReviewerId;

    /**
     * 指导老师审核状态：0-待审核 1-已通过 2-已拒绝
     */
    @TableField("teacher_review_status")
    private Integer teacherReviewStatus;

    /**
     * 指导老师审核意见
     */
    @TableField("teacher_review_comment")
    private String teacherReviewComment;

    /**
     * 指导老师审核时间
     */
    @TableField("teacher_review_time")
    private LocalDateTime teacherReviewTime;

    /**
     * 指导老师审核人
     */
    @TableField("teacher_reviewer_id")
    private Long teacherReviewerId;

    // ==================== 状态常量 ====================

    /**
     * 活动状态：未开始
     */
    public static final int STATUS_NOT_STARTED = 1;

    /**
     * 活动状态：报名中
     */
    public static final int STATUS_REGISTERING = 2;

    /**
     * 活动状态：进行中
     */
    public static final int STATUS_ONGOING = 3;

    /**
     * 活动状态：已结束
     */
    public static final int STATUS_ENDED = 4;

    /**
     * 审核状态：待审核
     */
    public static final int REVIEW_PENDING = 0;

    /**
     * 审核状态：已通过
     */
    public static final int REVIEW_APPROVED = 1;

    /**
     * 审核状态：已拒绝
     */
    public static final int REVIEW_REJECTED = 2;

    /**
     * 活动类型：线下活动
     */
    public static final int TYPE_OFFLINE = 1;

    /**
     * 活动类型：线上活动
     */
    public static final int TYPE_ONLINE = 2;

    /**
     * 活动类型：混合活动
     */
    public static final int TYPE_HYBRID = 3;
}
