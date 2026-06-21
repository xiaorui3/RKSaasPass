package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导航菜单实体类
 * 支持多租户
 */
@Data
@Accessors(chain = true)
@TableName("rk_navigation")
public class Navigation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父级ID(0表示顶级)
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序(数字越小越靠前)
     */
    private Integer sort;

    /**
     * 是否可见(0-隐藏 1-显示)
     */
    private Integer visible;

    /**
     * 打开方式(_self/_blank)
     */
    private String target;

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
