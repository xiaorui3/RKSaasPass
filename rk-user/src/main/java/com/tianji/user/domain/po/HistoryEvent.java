package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社团历史表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("club_history")
public class HistoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 事件日期
     */
    @TableField("event_date")
    private LocalDate eventDate;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 事件类型
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 重要程度
     */
    @TableField("importance_level")
    private Integer importanceLevel;

    /**
     * 相关成员（JSON格式）
     */
    @TableField("related_members")
    private String relatedMembers;

    /**
     * 标签（JSON格式）
     */
    private String tags;

    /**
     * 封面图片URL
     */
    @TableField("cover_image_url")
    private String coverImageUrl;

    /**
     * 是否为里程碑事件
     */
    @TableField("is_milestone")
    private Boolean isMilestone;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 额外信息（JSON格式）
     */
    @TableField("extra_info")
    private String extraInfo;

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