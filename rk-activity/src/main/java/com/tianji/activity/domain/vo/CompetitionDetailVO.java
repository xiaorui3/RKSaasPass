package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛详情VO
 * 用于比赛详情页展示
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("比赛详情")
public class CompetitionDetailVO implements Serializable {

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
     * 描述
     */
    @ApiModelProperty("比赛描述")
    private String description;

    /**
     * 内容详情
     */
    @ApiModelProperty("比赛内容详情（富文本）")
    private String content;

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
     * 主办方
     */
    @ApiModelProperty("主办方")
    private String organizer;

    /**
     * 联合主办方
     */
    @ApiModelProperty("联合主办方")
    private String coOrganizer;

    /**
     * 比赛地点
     */
    @ApiModelProperty("比赛地点")
    private String location;

    /**
     * 在线链接
     */
    @ApiModelProperty("在线比赛链接")
    private String onlineUrl;

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
     * 报名开始时间
     */
    @ApiModelProperty("报名开始时间")
    private LocalDateTime registrationStart;

    /**
     * 报名结束时间
     */
    @ApiModelProperty("报名结束时间")
    private LocalDateTime registrationEnd;

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
     * 最大参与人数
     */
    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    @ApiModelProperty("当前报名人数")
    private Integer registrationCount;

    /**
     * 规则文件
     */
    @ApiModelProperty("规则文件URL")
    private String rulesFile;

    /**
     * 材料文件
     */
    @ApiModelProperty("材料文件URL")
    private String materialsFile;

    /**
     * 结果文件
     */
    @ApiModelProperty("结果文件URL")
    private String resultsFile;

    /**
     * 奖项说明
     */
    @ApiModelProperty("奖项说明")
    private String awards;

    /**
     * 比赛总结
     */
    @ApiModelProperty("比赛总结")
    private String summary;

    /**
     * 联系人
     */
    @ApiModelProperty("联系人")
    private String contactPerson;

    /**
     * 联系电话
     */
    @ApiModelProperty("联系电话")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    /**
     * 标签
     */
    @ApiModelProperty("标签（JSON数组格式）")
    private String tags;

    /**
     * 是否已报名
     */
    @ApiModelProperty("当前用户是否已报名")
    private Boolean registered;

    /**
     * 报名状态（如果已报名）
     */
    @ApiModelProperty("报名状态：registered-已报名, checked_in-已签到, cancelled-已取消")
    private String registrationStatus;

    /**
     * 报名状态名称
     */
    @ApiModelProperty("报名状态名称")
    private String registrationStatusName;

    /**
     * 是否可以报名
     */
    @ApiModelProperty("是否可以报名")
    private Boolean canRegister;

    /**
     * 是否可以取消报名
     */
    @ApiModelProperty("是否可以取消报名")
    private Boolean canCancel;

    /**
     * 不能报名的原因
     */
    @ApiModelProperty("不能报名的原因")
    private String cannotRegisterReason;

    /**
     * 是否推荐
     */
    @ApiModelProperty("是否推荐")
    private Boolean isFeatured;

    /**
     * 是否发布
     */
    @ApiModelProperty("是否发布")
    private Boolean isPublished;

    @ApiModelProperty("是否允许跨租户展示/报名")
    private Boolean isCrossTenant;

    /**
     * 优先级
     */
    @ApiModelProperty("优先级")
    private Integer priority;

    /**
     * 浏览次数
     */
    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @ApiModelProperty("创建者")
    private String createdBy;

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

    /**
     * 获取报名状态名称
     */
    public String getRegistrationStatusName() {
        if (registrationStatus == null) return "";
        switch (registrationStatus) {
            case "registered": return "已报名";
            case "checked_in": return "已签到";
            case "cancelled": return "已取消";
            default: return registrationStatus;
        }
    }
}
