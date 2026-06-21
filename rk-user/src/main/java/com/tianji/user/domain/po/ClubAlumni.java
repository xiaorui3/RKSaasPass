package com.tianji.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 校友表（支持多租户）
 * 基于企业级ERP多租户标准
 */
@Data
@Accessors(chain = true)
@TableName("club_alumni")
public class ClubAlumni implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（企业级多租户标准）
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 英文名
     */
    @TableField("english_name")
    private String englishName;

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
     * 入学年份
     */
    @TableField("generation_year")
    private Integer generationYear;

    /**
     * 职位
     */
    private String position;

    /**
     * 部门
     */
    private String department;

    /**
     * 年级班级
     */
    @TableField("grade_class")
    private String gradeClass;

    /**
     * 入学年份
     */
    @TableField("enrollment_year")
    private Integer enrollmentYear;

    /**
     * 预计毕业年份
     */
    @TableField("expected_graduation_year")
    private Integer expectedGraduationYear;

    /**
     * 实际毕业日期
     */
    @TableField("actual_graduation_date")
    private LocalDate actualGraduationDate;

    /**
     * 专业
     */
    private String major;

    /**
     * 技能
     */
    private String skills;

    /**
     * 荣誉证书
     */
    @TableField("honor_certificates")
    private String honorCertificates;

    /**
     * 工作城市
     */
    @TableField("work_city")
    private String workCity;

    /**
     * 工作单位
     */
    @TableField("work_unit")
    private String workUnit;

    /**
     * 工作内容
     */
    @TableField("job_content")
    private String jobContent;

    /**
     * 当前联系方式
     */
    @TableField("current_contact")
    private String currentContact;

    /**
     * 备注
     */
    private String notes;

    /**
     * 建议
     */
    private String advice;

    /**
     * 是否显示表格
     */
    @TableField("show_table")
    private Boolean showTable;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 是否核心成员
     */
    @TableField("is_core_member")
    private Boolean isCoreMember;

    /**
     * 成员状态
     */
    @TableField("member_status")
    private String memberStatus;

    @TableField("graduation_status")
    private String graduationStatus;

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
