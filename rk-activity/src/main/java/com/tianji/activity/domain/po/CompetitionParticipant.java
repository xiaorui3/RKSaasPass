package com.tianji.activity.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛参与者/报名实体类（支持多租户）
 * 对应数据库表：competition_participants
 */
@Data
@Accessors(chain = true)
@TableName("competition_participants")
public class CompetitionParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参与者ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 比赛ID
     */
    @TableField("competition_id")
    private Long competitionId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

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
     * 手机号
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
     * 相关经验
     */
    private String experience;

    /**
     * 队伍名称（团队赛）
     */
    @TableField("team_name")
    private String teamName;

    /**
     * 队伍人数（团队赛）
     */
    @TableField("team_size")
    private Integer teamSize;

    /**
     * 队员信息JSON（团队赛）
     */
    @TableField("team_members")
    private String teamMembers;

    /**
     * 报名时间
     */
    @TableField("registration_time")
    private LocalDateTime registrationTime;

    /**
     * 状态：registered-已报名，approved-已通过，rejected-已拒绝，cancelled-已取消
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 奖项（1一等奖 2二等奖 3三等奖 4优秀奖）
     */
    private Integer award;

    /**
     * 排名
     */
    @TableField("ranking")
    private Integer ranking;

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
     * 删除标记
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    // ==================== 状态常量 ====================

    /**
     * 状态：已报名
     */
    public static final String STATUS_REGISTERED = "registered";

    /**
     * 状态：已通过
     */
    public static final String STATUS_APPROVED = "approved";

    /**
     * 状态：已拒绝
     */
    public static final String STATUS_REJECTED = "rejected";

    /**
     * 状态：已取消
     */
    public static final String STATUS_CANCELLED = "cancelled";
}
