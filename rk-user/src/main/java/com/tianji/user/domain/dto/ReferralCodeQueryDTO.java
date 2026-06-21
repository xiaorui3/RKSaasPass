package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 内推码查询DTO
 */
@Data
@ApiModel("内推码查询条件")
public class ReferralCodeQueryDTO {

    @ApiModelProperty("内推码（模糊查询）")
    private String code;

    @ApiModelProperty("生成者ID")
    private Long generatorId;

    @ApiModelProperty("状态: 1-有效 0-失效")
    private Integer status;

    @ApiModelProperty("当前页码")
    private Integer page = 1;

    @ApiModelProperty("每页大小")
    private Integer size = 10;

    @ApiModelProperty("排序字段")
    private String sortBy = "create_time";

    @ApiModelProperty("排序方向")
    private String sortOrder = "desc";
}
