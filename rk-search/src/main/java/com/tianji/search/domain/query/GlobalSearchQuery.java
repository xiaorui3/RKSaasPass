package com.tianji.search.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "全站搜索条件")
public class GlobalSearchQuery extends PageQuery {

    @ApiModelProperty(value = "搜索关键词", example = "黑河学院")
    private String keyword;

    @ApiModelProperty(value = "实体类型过滤，例如 NEWS、ACTIVITY、COMPETITION、WORKS、TENANT、MEMBER、ALUMNI、HISTORY、NOTICE")
    private List<String> entityTypes;

    @ApiModelProperty(value = "租户ID，不传时使用当前请求租户")
    private Long tenantId;

    @ApiModelProperty(value = "是否跨租户搜索，仅超级管理员请求生效")
    private Boolean includeAllTenants;
}
