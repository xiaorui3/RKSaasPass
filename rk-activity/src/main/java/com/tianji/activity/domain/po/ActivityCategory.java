package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 活动分类实体类（支持多租户）
 * 对应数据库表：rk_activity.rk_activity_category
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("rk_activity_category")
public class ActivityCategory extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父分类ID（0表示顶级分类）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类编码
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序（数字越小越靠前）
     */
    private Integer sort;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 状态常量：启用
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 状态常量：禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 默认父ID（顶级分类）
     */
    public static final long ROOT_PARENT_ID = 0L;
}
