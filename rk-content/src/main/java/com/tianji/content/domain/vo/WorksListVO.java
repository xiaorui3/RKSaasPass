package com.tianji.content.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品列表视图对象
 * 用于作品列表展示，包含列表所需的基本信息
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "作品列表视图对象")
public class WorksListVO {

    @ApiModelProperty("作品ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("演示视频URL")
    private String demoVideo;

    @ApiModelProperty("是否精选")
    private Boolean isFeatured;

    @ApiModelProperty("展示顺序")
    private Integer displayOrder;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("点赞次数")
    private Integer likeCount;

    @ApiModelProperty("技术栈（JSON格式）")
    private String technologies;

    @ApiModelProperty("作者（JSON格式）")
    private String authors;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
