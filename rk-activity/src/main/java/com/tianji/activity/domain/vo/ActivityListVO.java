package com.tianji.activity.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动列表VO
 * 用于活动列表页展示
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动列表项")
public class ActivityListVO implements Serializable {

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
     * 封面图片
     */
    @ApiModelProperty("封面图片")
    private String coverImage;

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
     * 是否已报名
     */
    @ApiModelProperty("当前用户是否已报名")
    private Boolean registered;

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
}
