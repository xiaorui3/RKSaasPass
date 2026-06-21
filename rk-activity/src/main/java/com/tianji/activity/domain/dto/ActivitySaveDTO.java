package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动保存DTO
 * 用于新增/修改活动信息
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动保存请求")
public class ActivitySaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活动ID（修改时必填）
     */
    @ApiModelProperty("活动ID（修改时必填）")
    private Long id;

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名称不能为空")
    @ApiModelProperty(value = "活动名称", required = true)
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
     * 封面图片URL
     */
    @ApiModelProperty("封面图片URL")
    private String coverImage;

    /**
     * 活动类型
     */
    @NotNull(message = "活动类型不能为空")
    @ApiModelProperty(value = "活动类型：1-讲座, 2-比赛, 3-培训, 4-会议, 5-户外活动, 6-其他", required = true)
    private Integer activityType;

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
     * 最大参与人数
     */
    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    /**
     * 活动开始时间
     */
    @NotNull(message = "活动开始时间不能为空")
    @ApiModelProperty(value = "活动开始时间", required = true)
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    @NotNull(message = "活动结束时间不能为空")
    @ApiModelProperty(value = "活动结束时间", required = true)
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
     * 是否置顶
     */
    @ApiModelProperty("是否置顶：1-是, 0-否")
    private Integer isTop;

    /**
     * 是否热门
     */
    @ApiModelProperty("是否热门：1-是, 0-否")
    private Integer isHot;
}
