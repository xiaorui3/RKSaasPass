package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 新闻查询参数DTO
 * 用于分页查询新闻列表的参数封装
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "新闻查询参数DTO")
public class NewsQueryDTO {

    @ApiModelProperty("当前页码，默认1")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @ApiModelProperty("每页大小，默认10")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;

    @ApiModelProperty("分类筛选")
    private String category;

    @ApiModelProperty("搜索关键词")
    private String keyword;

    @ApiModelProperty("作者筛选")
    private String author;

    @ApiModelProperty("发布状态：0-草稿，1-发布")
    private Integer isPublished;

    @ApiModelProperty("是否置顶：0-否，1-是")
    private Integer isFeatured;

    @ApiModelProperty("是否刷新缓存")
    private Boolean refresh = false;

    @ApiModelProperty("排序字段，默认publishTime")
    private String sortBy = "publishTime";

    @ApiModelProperty("排序方向，默认desc")
    private String sortOrder = "desc";
}
