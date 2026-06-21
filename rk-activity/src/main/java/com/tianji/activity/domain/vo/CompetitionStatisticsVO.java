package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛统计视图对象
 * 用于管理后台统计数据展示
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("比赛统计信息")
public class CompetitionStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 比赛ID
     */
    @ApiModelProperty("比赛ID")
    private Long competitionId;

    /**
     * 比赛标题
     */
    @ApiModelProperty("比赛标题")
    private String title;

    // ==================== 报名统计 ====================

    /**
     * 总报名人数
     */
    @ApiModelProperty("总报名人数")
    private Integer totalRegistrations;

    /**
     * 已签到人数
     */
    @ApiModelProperty("已签到人数")
    private Integer checkedInCount;

    /**
     * 已取消人数
     */
    @ApiModelProperty("已取消人数")
    private Integer cancelledCount;

    /**
     * 待签到人数
     */
    @ApiModelProperty("待签到人数")
    private Integer pendingCount;

    /**
     * 最大参与人数
     */
    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    /**
     * 报名率（报名人数/最大人数）
     */
    @ApiModelProperty("报名率（百分比）")
    private Double registrationRate;

    /**
     * 签到率（签到人数/报名人数）
     */
    @ApiModelProperty("签到率（百分比）")
    private Double checkInRate;

    // ==================== 类型统计 ====================

    /**
     * 个人赛报名人数
     */
    @ApiModelProperty("个人赛报名人数")
    private Integer individualCount;

    /**
     * 团队赛报名队伍数
     */
    @ApiModelProperty("团队赛报名队伍数")
    private Integer teamCount;

    /**
     * 团队赛总人数
     */
    @ApiModelProperty("团队赛总人数")
    private Integer teamMemberCount;

    // ==================== 学院/专业分布 ====================

    /**
     * 报名人数最多的学院
     */
    @ApiModelProperty("报名人数最多的学院")
    private String topCollege;

    /**
     * 该学院报名人数
     */
    @ApiModelProperty("该学院报名人数")
    private Integer topCollegeCount;

    /**
     * 报名人数最多的专业
     */
    @ApiModelProperty("报名人数最多的专业")
    private String topMajor;

    /**
     * 该专业报名人数
     */
    @ApiModelProperty("该专业报名人数")
    private Integer topMajorCount;

    // ==================== 年级分布 ====================

    /**
     * 大一报名人数
     */
    @ApiModelProperty("大一报名人数")
    private Integer grade1Count;

    /**
     * 大二报名人数
     */
    @ApiModelProperty("大二报名人数")
    private Integer grade2Count;

    /**
     * 大三报名人数
     */
    @ApiModelProperty("大三报名人数")
    private Integer grade3Count;

    /**
     * 大四报名人数
     */
    @ApiModelProperty("大四报名人数")
    private Integer grade4Count;

    /**
     * 研究生报名人数
     */
    @ApiModelProperty("研究生报名人数")
    private Integer graduateCount;

    // ==================== 其他统计 ====================

    /**
     * 浏览次数
     */
    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    /**
     * 比赛状态
     */
    @ApiModelProperty("比赛状态")
    private String status;

    /**
     * 状态名称
     */
    @ApiModelProperty("状态名称")
    private String statusName;

    /**
     * 获取状态名称
     */
    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case "DRAFT": return "草稿";
            case "PUBLISHED": return "已发布";
            case "ONGOING": return "进行中";
            case "COMPLETED": return "已结束";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
}
