package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户详情表（支持多租户）
 * </p>
 *
 * @author 虎哥
 * @since 2022-08-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rk_user_detail")
public class UserDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 身份证号
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 地址
     */
    private String address;

    /**
     * 家乡
     */
    private String hometown;

    /**
     * 政治面貌
     */
    @TableField("political_status")
    private String politicalStatus;

    /**
     * 民族
     */
    private String nation;

    /**
     * 个人介绍
     */
    private String introduction;

    /**
     * 技能
     */
    private String skills;

    /**
     * 爱好
     */
    private String hobbies;

    /**
     * 角色ID（不在rk_user_detail表中）
     */
    @TableField(exist = false)
    private Long roleId;

    /**
     * 用户类型（不在rk_user_detail表中）
     */
    @TableField(exist = false)
    private String type;

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
     * 姓名
     */
    @TableField(exist = false)
    private String name;

    /**
     * 头像
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 职位
     */
    @TableField(exist = false)
    private String job;

    /**
     * 照片
     */
    @TableField(exist = false)
    private String photo;

    /**
     * 邮箱
     */
    @TableField(exist = false)
    private String email;

    /**
     * QQ号
     */
    @TableField(exist = false)
    private String qq;

    /**
     * 省份
     */
    @TableField(exist = false)
    private String province;

    /**
     * 城市
     */
    @TableField(exist = false)
    private String city;

    /**
     * 区县
     */
    @TableField(exist = false)
    private String district;

    /**
     * 个人介绍
     */
    @TableField(exist = false)
    private String intro;

    @TableField(exist = false)
    private String cellPhone;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private Integer status;
}