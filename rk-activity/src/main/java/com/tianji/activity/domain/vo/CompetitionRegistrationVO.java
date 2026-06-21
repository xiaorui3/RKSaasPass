package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛报名视图对象
 * 用于展示报名信息
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("比赛报名信息")
public class CompetitionRegistrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报名ID
     */
    @ApiModelProperty("报名ID")
    private Long id;

    /**
     * 比赛ID
     */
    @ApiModelProperty("比赛ID")
    private Long competitionId;

    /**
     * 比赛标题
     */
    @ApiModelProperty("比赛标题")
    private String competitionTitle;

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 姓名
     */
    @ApiModelProperty("姓名")
    private String name;

    /**
     * 学号
     */
    @ApiModelProperty("学号")
    private String studentId;

    /**
     * 邮箱
     */
    @ApiModelProperty("邮箱")
    private String email;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String phone;

    /**
     * 专业
     */
    @ApiModelProperty("专业")
    private String major;

    /**
     * 年级
     */
    @ApiModelProperty("年级")
    private String grade;

    /**
     * 队伍名称（团队赛）
     */
    @ApiModelProperty("队伍名称")
    private String teamName;

    /**
     * 队伍人数（团队赛）
     */
    @ApiModelProperty("队伍人数")
    private Integer teamSize;

    /**
     * 队员信息
     */
    @ApiModelProperty("队员信息（JSON格式）")
    private String teamMembers;

    /**
     * 报名时间
     */
    @ApiModelProperty("报名时间")
    private LocalDateTime registrationTime;

    /**
     * 签到时间
     */
    @ApiModelProperty("签到时间")
    private LocalDateTime checkInTime;

    /**
     * 状态
     */
    @ApiModelProperty("状态：registered-已报名, checked_in-已签到, cancelled-已取消")
    private String status;

    /**
     * 状态名称
     */
    @ApiModelProperty("状态名称")
    private String statusName;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 获取状态名称
     */
    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case "registered": return "已报名";
            case "checked_in": return "已签到";
            case "cancelled": return "已取消";
            default: return status;
        }
    }
}
