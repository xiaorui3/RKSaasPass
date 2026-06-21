package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.tianji.common.enums.UserType;
import com.tianji.user.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表（支持多租户）
 * </p>
 *
 * @author 虎哥
 * @since 2022-06-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rk_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * auth服务用户ID，用于关联rk_auth.user
     */
    @TableField("auth_user_id")
    private Long authUserId;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 性别：1-男 2-女
     */
    private Integer gender;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String cellPhone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 学号
     */
    @TableField("student_id")
    private String studentId;

    /**
     * 学院
     */
    private String college;

    /**
     * 专业
     */
    private String major;

    /**
     * 年级
     */
    private String grade;

    /**
     * 用户类型：1-学生 2-教师 3-管理员
     */
    @TableField("user_type")
    private UserType type;

    /**
     * 状态：1-正常 0-禁用
     */
    private UserStatus status;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private LocalDateTime joinTime;

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
     * 删除标记：0-未删除 1-已删除（企业级多租户标准）
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}