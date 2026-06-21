package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知实体类
 * 支持多租户
 */
@Data
@Accessors(chain = true)
@TableName("rk_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 类型(system/activity/message)
     */
    private String type;

    /**
     * 优先级(0-普通 1-重要 2-紧急)
     */
    private Integer priority;

    /**
     * 发送者ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 发送者名称
     */
    @TableField("sender_name")
    private String senderName;

    /**
     * 目标类型(0-全部用户 1-指定用户 2-指定角色)
     */
    @TableField("target_type")
    private Integer targetType;

    /**
     * 目标ID列表(JSON数组)
     */
    @TableField("target_ids")
    private String targetIds;

    @TableField("metadata")
    private String metadata;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

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
     * 删除标记(0-未删除 1-已删除)
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
