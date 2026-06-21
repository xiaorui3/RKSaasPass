package com.tianji.search.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "全站搜索结果")
public class GlobalSearchResultVO {

    @ApiModelProperty("实体类型")
    private String entityType;

    @ApiModelProperty("实体ID")
    private Long entityId;

    @ApiModelProperty("租户ID")
    private Long tenantId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("跳转路由")
    private String route;

    @ApiModelProperty("封面图")
    private String coverUrl;

    @ApiModelProperty("更新时间")
    private String updatedAt;

    @ApiModelProperty("高亮片段")
    private String highlight;
}
