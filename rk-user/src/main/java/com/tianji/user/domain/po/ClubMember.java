package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社团成员表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("club_members")
public class ClubMember implements Serializable {

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
     * 姓名
     */
    private String name;

    /**
     * 学号
     */
    @TableField("student_id")
    private String studentId;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;

    /**
     * 专业
     */
    private String major;

    /**
     * 年级
     */
    private String grade;

    /**
     * 部门
     */
    private String department;

    /**
     * 职位
     */
    private String position;

    /**
     * 加入日期
     */
    @TableField("join_date")
    private LocalDateTime joinDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 删除标记
     */
    @TableField(exist = false)
    private Integer deleteFlag;

    /**
     * 提交时间
     */
    @TableField(exist = false)
    private LocalDateTime submitTime;

    /**
     * 审核状态
     */
    @TableField(exist = false)
    private Integer agreeStatus;

    /**
     * 审核评论
     */
    @TableField(exist = false)
    private String reviewComment;

    /**
     * 审核时间
     */
    @TableField(exist = false)
    private LocalDateTime reviewTime;

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