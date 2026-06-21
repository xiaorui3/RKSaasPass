package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动报名实体类（支持多租户）
 * 对应数据库表：rk_activity.rk_activity_registration
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("rk_activity_registration")
public class ActivityRegistration extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID
     */
    @TableField("activity_id")
    private Long activityId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 报名状态：1-已报名 2-已签到 3-已取消
     */
    @TableField("registration_status")
    private Integer registrationStatus;

    /**
     * 报名时间
     */
    @TableField("registration_time")
    private LocalDateTime registrationTime;

    /**
     * 签到时间
     */
    @TableField("check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 取消时间
     */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 报名状态：已报名
     */
    public static final int STATUS_REGISTERED = 1;

    /**
     * 报名状态：已签到
     */
    public static final int STATUS_CHECKED_IN = 2;

    /**
     * 报名状态：已取消
     */
    public static final int STATUS_CANCELLED = 3;
}
