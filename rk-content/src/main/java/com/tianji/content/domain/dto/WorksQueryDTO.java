package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 作品查询参数DTO
 * 用于分页查询作品列表的参数封装
 *
 * @author RK-Web
 * @since 2026/03/13
 */
@Data
@ApiModel(description = "作品查询参数DTO")
public class WorksQueryDTO {

    @ApiModelProperty("当前页码，默认1")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @ApiModelProperty("每页大小，默认10")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;

    @ApiModelProperty("分类筛选：web/mobile/ai/desktop/other")
    private String category;

    @ApiModelProperty("搜索关键词（标题/描述）")
    private String keyword;

    @ApiModelProperty("作者筛选")
    private String author;

    @ApiModelProperty("是否精选：true-是，false-否")
    private Boolean isFeatured;

    @ApiModelProperty("是否刷新缓存")
    private Boolean refresh = false;

    @ApiModelProperty("排序字段，默认createTime")
    private String sortBy = "createTime";

    @ApiModelProperty("排序方向，默认desc")
    private String sortOrder = "desc";

    @ApiModelProperty("技术栈筛选")
    private String technology;
}
