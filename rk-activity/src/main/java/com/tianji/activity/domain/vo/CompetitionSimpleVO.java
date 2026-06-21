package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛简单视图对象
 * 用于下拉选择、关联展示等场景
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("比赛简要信息")
public class CompetitionSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 比赛ID
     */
    @ApiModelProperty("比赛ID")
    private Long id;

    /**
     * 比赛标题
     */
    @ApiModelProperty("比赛标题")
    private String title;

    /**
     * 副标题
     */
    @ApiModelProperty("副标题")
    private String subtitle;

    /**
     * 封面图片
     */
    @ApiModelProperty("封面图片URL")
    private String coverImage;

    /**
     * 比赛类型
     */
    @ApiModelProperty("比赛类型")
    private String competitionType;

    /**
     * 比赛类型名称
     */
    @ApiModelProperty("比赛类型名称")
    private String competitionTypeName;

    /**
     * 比赛级别
     */
    @ApiModelProperty("比赛级别")
    private String level;

    /**
     * 比赛级别名称
     */
    @ApiModelProperty("比赛级别名称")
    private String levelName;

    /**
     * 状态
     */
    @ApiModelProperty("状态")
    private String status;

    /**
     * 状态名称
     */
    @ApiModelProperty("状态名称")
    private String statusName;

    /**
     * 比赛开始时间
     */
    @ApiModelProperty("比赛开始时间")
    private LocalDateTime competitionStart;

    /**
     * 比赛结束时间
     */
    @ApiModelProperty("比赛结束时间")
    private LocalDateTime competitionEnd;

    /**
     * 主办方
     */
    @ApiModelProperty("主办方")
    private String organizer;

    /**
     * 是否推荐
     */
    @ApiModelProperty("是否推荐")
    private Boolean isFeatured;

    /**
     * 获取比赛类型名称
     */
    public String getCompetitionTypeName() {
        if (competitionType == null) return "其他";
        switch (competitionType) {
            case "academic": return "学术竞赛";
            case "innovation": return "创新创业";
            case "sports": return "体育竞技";
            case "art": return "艺术比赛";
            case "skill": return "技能竞赛";
            default: return competitionType;
        }
    }

    /**
     * 获取比赛级别名称
     */
    public String getLevelName() {
        if (level == null) return "未设置";
        switch (level) {
            case "school": return "校级";
            case "city": return "市级";
            case "province": return "省级";
            case "national": return "国家级";
            case "international": return "国际级";
            default: return level;
        }
    }

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
