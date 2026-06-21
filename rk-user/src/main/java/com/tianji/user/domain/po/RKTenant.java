package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户实体类
 * 对应数据库表：rk_user.rk_tenant
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("rk_tenant")
public class RKTenant {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * Logo URL
     */
    private String logoUrl;

    /**
     * Display order
     */
    @TableField("display_order")
    private Integer displayOrder;

    /**
     * 描述
     */
    private String description;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 状态：1-正常 0-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField("creator")
    private Long creator;

    /**
     * 更新者
     */
    @TableField("updater")
    private Long updater;

    /**
     * 删除标记
     */
    @TableField("is_deleted")
    private Integer isDeleted;
}
