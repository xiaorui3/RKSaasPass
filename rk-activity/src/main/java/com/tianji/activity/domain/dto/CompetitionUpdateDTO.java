package com.tianji.activity.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("比赛更新请求")
public class CompetitionUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "比赛ID不能为空")
    @ApiModelProperty(value = "比赛ID", required = true)
    private Long id;

    @ApiModelProperty("比赛标题")
    private String title;

    @ApiModelProperty("副标题")
    private String subtitle;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("内容详情")
    private String content;

    @ApiModelProperty("主办方")
    private String organizer;

    @ApiModelProperty("联合主办方")
    private String coOrganizer;

    @ApiModelProperty("比赛类型")
    private String competitionType;

    @ApiModelProperty("比赛级别")
    private String level;

    @ApiModelProperty("最大参与人数")
    private Integer maxParticipants;

    @ApiModelProperty("报名开始时间")
    private LocalDateTime registrationStart;

    @ApiModelProperty("报名结束时间")
    private LocalDateTime registrationEnd;

    @ApiModelProperty("比赛开始时间")
    private LocalDateTime competitionStart;

    @ApiModelProperty("比赛结束时间")
    private LocalDateTime competitionEnd;

    @ApiModelProperty("比赛地点")
    private String location;

    @ApiModelProperty("线上链接")
    private String onlineUrl;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("封面图片")
    private String coverImage;

    @ApiModelProperty("规则文件")
    private String rulesFile;

    @ApiModelProperty("材料文件")
    private String materialsFile;

    @ApiModelProperty("结果文件")
    private String resultsFile;

    @ApiModelProperty("总结")
    private String summary;

    @ApiModelProperty("奖项说明")
    private String awards;

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

    @ApiModelProperty("联系人")
    private String contactPerson;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    @ApiModelProperty("标签")
    private String tags;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("是否推荐")
    private Boolean isFeatured;

    @ApiModelProperty("是否发布")
    private Boolean isPublished;

    @ApiModelProperty("是否允许跨租户展示/报名")
    private Boolean isCrossTenant;

    @ApiModelProperty("负责人审批人ID")
    private Long managerReviewerId;

    @ApiModelProperty("指导老师审批人ID")
    private Long teacherReviewerId;
}
