package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图实体类
 * 支持多租户
 */
@Data
@Accessors(chain = true)
@TableName("rk_banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 图片地址
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 跳转链接
     */
    @TableField("link_url")
    private String linkUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序(数字越小越靠前)
     */
    private Integer sort;

    /**
     * 类型(1:首页轮播 2:活动轮播)
     */
    private Integer type;

    /**
     * 状态(0:禁用 1:启用)
     */
    private Integer status;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

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
