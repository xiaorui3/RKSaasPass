package com.tianji.content.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻列表视图对象
 * 用于新闻列表展示，包含列表所需的基本信息
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "新闻列表视图对象")
public class NewsListVO {

    @ApiModelProperty("新闻ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("视频URL")
    private String videoUrl;

    @ApiModelProperty("附件URL")
    private String attachmentUrl;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("标签")
    private String tags;

    @ApiModelProperty("是否发布：0-草稿，1-发布")
    private Integer isPublished;

    @ApiModelProperty("是否置顶：0-否，1-是")
    private Integer isFeatured;

    @ApiModelProperty("是否允许跨租户公开展示：0-否，1-是")
    private Integer isCrossTenant;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("发布时间")
    private LocalDateTime publishTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("审核状态：0-草稿 1-待审核 2-已通过 3-已拒绝")
    private Integer approvalStatus;

    @ApiModelProperty("审核人")
    private String approver;

    @ApiModelProperty("审核时间")
    private LocalDateTime approvalTime;

    @ApiModelProperty("拒绝原因")
    private String rejectReason;
}
