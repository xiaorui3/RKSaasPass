package com.tianji.content.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品简单视图对象
 * 用于首页、侧边栏、推荐等位置的简化展示
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "作品简单视图对象")
public class WorksSimpleVO {

    @ApiModelProperty("作品ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("是否精选")
    private Boolean isFeatured;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("点赞次数")
    private Integer likeCount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
