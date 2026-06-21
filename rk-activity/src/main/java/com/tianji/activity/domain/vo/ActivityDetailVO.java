package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动详情VO
 * 用于活动详情页展示
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动详情")
public class ActivityDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID
     */
    @ApiModelProperty("活动ID")
    private Long id;

    /**
     * 活动名称
     */
    @ApiModelProperty("活动名称")
    private String activityName;

    /**
     * 活动编码
     */
    @ApiModelProperty("活动编码")
    private String activityCode;

    /**
     * 活动分类ID
     */
    @ApiModelProperty("活动分类ID")
    private Long categoryId;

    /**
     * 活动分类名称
     */
    @ApiModelProperty("活动分类名称")
    private String categoryName;

    /**
     * 封面图片
     */
    @ApiModelProperty("封面图片")
    private String coverImage;

    /**
     * 附件URL
     */
    @ApiModelProperty("附件URL")
    private String attachmentUrl;

    /**
     * 活动类型
     */
    @ApiModelProperty("活动类型：1-讲座, 2-比赛, 3-培训, 4-会议, 5-户外活动, 6-其他")
    private Integer activityType;

    /**
     * 活动类型名称
     */
    @ApiModelProperty("活动类型名称")
    private String activityTypeName;

    /**
     * 主办方
     */
    @ApiModelProperty("主办方")
    private String organizer;

    /**
     * 活动地点
     */
    @ApiModelProperty("活动地点")
    private String location;

    /**
     * 线上活动链接
     */
    @ApiModelProperty("线上活动链接")
    private String onlineUrl;

    /**
     * 活动状态
     */
    @ApiModelProperty("活动状态：1-未开始, 2-报名中, 3-进行中, 4-已结束")
    private Integer activityStatus;

    /**
     * 活动状态名称
     */
    @ApiModelProperty("活动状态名称")
    private String activityStatusName;

    /**
     * 活动开始时间
     */
    @ApiModelProperty("活动开始时间")
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    @ApiModelProperty("活动结束时间")
    private LocalDateTime endTime;

    /**
     * 报名开始时间
     */
    @ApiModelProperty("报名开始时间")
    private LocalDateTime registrationStartTime;

    /**
     * 报名结束时间
     */
    @ApiModelProperty("报名结束时间")
    private LocalDateTime registrationEndTime;

    /**
     * 最大参与人数
     */
    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    /**
     * 当前参与人数
     */
    @ApiModelProperty("当前参与人数")
    private Integer currentParticipants;

    /**
     * 活动内容详情
     */
    @ApiModelProperty("活动内容详情")
    private String content;

    /**
     * 参与要求
     */
    @ApiModelProperty("参与要求")
    private String requirements;

    /**
     * 活动积分
     */
    @ApiModelProperty("活动积分")
    private Integer points;

    /**
     * 是否已报名
     */
    @ApiModelProperty("当前用户是否已报名")
    private Boolean registered;

    /**
     * 报名状态（如果已报名）
     */
    @ApiModelProperty("报名状态：1-已报名, 2-已签到, 3-已取消, 4-已缺席")
    private Integer registrationStatus;

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
     * 是否可以签到
     */
    @ApiModelProperty("是否可以签到")
    private Boolean canCheckIn;

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
     * 是否置顶
     */
    @ApiModelProperty("是否置顶")
    private Boolean isTop;

    /**
     * 是否热门
     */
    @ApiModelProperty("是否热门")
    private Boolean isHot;

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
     * 创建者名称
     */
    @ApiModelProperty("创建者名称")
    private String creatorName;

    /**
     * 获取活动类型名称
     */
    public String getActivityTypeName() {
        if (activityType == null) return "未知";
        switch (activityType) {
            case 1: return "讲座";
            case 2: return "比赛";
            case 3: return "培训";
            case 4: return "会议";
            case 5: return "户外活动";
            case 6: return "其他";
            default: return "未知";
        }
    }

    /**
     * 获取活动状态名称
     */
    public String getActivityStatusName() {
        if (activityStatus == null) return "未知";
        switch (activityStatus) {
            case 1: return "未开始";
            case 2: return "报名中";
            case 3: return "进行中";
            case 4: return "已结束";
            default: return "未知";
        }
    }

    /**
     * 获取报名状态名称
     */
    public String getRegistrationStatusName() {
        if (registrationStatus == null) return "";
        switch (registrationStatus) {
            case 1: return "已报名";
            case 2: return "已签到";
            case 3: return "已取消";
            case 4: return "已缺席";
            default: return "";
        }
    }
}
