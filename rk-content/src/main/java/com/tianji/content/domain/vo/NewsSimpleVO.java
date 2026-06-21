package com.tianji.content.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻简单视图对象
 * 用于首页、侧边栏等位置的简化展示
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "新闻简单视图对象")
public class NewsSimpleVO {

    @ApiModelProperty("新闻ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("是否置顶：0-否，1-是")
    private Integer isFeatured;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("发布时间")
    private LocalDateTime publishTime;
}
