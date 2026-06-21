package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("比赛列表页")
public class CompetitionListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("比赛ID")
    private Long id;

    @ApiModelProperty("租户ID")
    private Long tenantId;

    @ApiModelProperty("比赛标题")
    private String title;

    @ApiModelProperty("副标题")
    private String subtitle;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("封面")
    private String coverImage;

    @ApiModelProperty("比赛类型")
    private String competitionType;

    @ApiModelProperty("比赛类型名称")
    private String competitionTypeName;

    @ApiModelProperty("比赛级别")
    private String level;

    @ApiModelProperty("比赛级别名称")
    private String levelName;

    @ApiModelProperty("主办方")
    private String organizer;

    @ApiModelProperty("地点")
    private String location;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态名称")
    private String statusName;

    @ApiModelProperty("报名开始时间")
    private LocalDateTime registrationStart;

    @ApiModelProperty("报名结束时间")
    private LocalDateTime registrationEnd;

    @ApiModelProperty("比赛开始时间")
    private LocalDateTime competitionStart;

    @ApiModelProperty("比赛结束时间")
    private LocalDateTime competitionEnd;

    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    @ApiModelProperty("当前报名人数")
    private Integer registrationCount;

    @ApiModelProperty("当前用户是否已报名")
    private Boolean registered;

    @ApiModelProperty("是否可以报名")
    private Boolean canRegister;

    @ApiModelProperty("是否推荐")
    private Boolean isFeatured;

    @ApiModelProperty("是否发布")
    private Boolean isPublished;

    @ApiModelProperty("是否允许跨租户展示/报名")
    private Boolean isCrossTenant;

    @ApiModelProperty("参赛学分")
    private Integer participationPoints;

    @ApiModelProperty("一等奖学分")
    private Integer firstPrizePoints;

    @ApiModelProperty("二等奖学分")
    private Integer secondPrizePoints;

    @ApiModelProperty("三等奖学分")
    private Integer thirdPrizePoints;

    @ApiModelProperty("优秀奖学分")
    private Integer excellentPrizePoints;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("标签")
    private String tags;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    public String getCompetitionTypeName() {
        if (competitionType == null) return "其他";
        switch (competitionType) {
            case "academic": return "学术竞赛";
            case "innovation": return "创新创业";
            case "sports": return "体育竞技";
            case "art": return "艺术比赛";
            case "skill": return "技能竞赛";
            case "coding":
            case "编程":
                return "编程竞赛";
            case "design":
            case "设计":
                return "设计竞赛";
            default: return competitionType;
        }
    }

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

    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case "DRAFT": return "草稿";
            case "PUBLISHED": return "已发布";
            case "REGISTRATION": return "报名中";
            case "ONGOING": return "进行中";
            case "COMPLETED": return "已结束";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
}
