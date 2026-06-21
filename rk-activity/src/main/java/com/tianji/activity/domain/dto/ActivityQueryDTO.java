package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 活动查询DTO
 * 用于活动列表的条件查询
 * 
 * @author RK-Web Team
 * @since 1.0.0
 */
@Data
@ApiModel("活动查询条件")
public class ActivityQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @ApiModelProperty("页码，默认1")
    private Integer page = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty("每页数量，默认10")
    private Integer size = 10;

    /**
     * 活动状态（1-未开始, 2-报名中, 3-进行中, 4-已结束）
     */
    @ApiModelProperty("活动状态：1-未开始, 2-报名中, 3-进行中, 4-已结束")
    private Integer status;

    /**
     * 活动类型（1-讲座, 2-比赛, 3-培训, 4-会议, 5-户外活动, 6-其他）
     */
    @ApiModelProperty("活动类型：1-讲座, 2-比赛, 3-培训, 4-会议, 5-户外活动, 6-其他")
    private Integer type;

    /**
     * 活动分类ID
     */
    @ApiModelProperty("活动分类ID")
    private Long categoryId;

    /**
     * 搜索关键词（活动名称/组织者）
     */
    @ApiModelProperty("搜索关键词")
    private String keyword;

    /**
     * 是否只查询热门活动
     */
    @ApiModelProperty("是否只查询热门活动")
    private Boolean hotOnly;

    /**
     * 是否只查询置顶活动
     */
    @ApiModelProperty("是否只查询置顶活动")
    private Boolean topOnly;

    /**
     * 开始时间筛选
     */
    @ApiModelProperty("活动开始时间-起")
    private String startTimeBegin;

    /**
     * 结束时间筛选
     */
    @ApiModelProperty("活动开始时间-止")
    private String startTimeEnd;
}
