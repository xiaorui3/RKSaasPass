package com.tianji.content.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 新闻审核通过DTO
 * 用于审核通过新闻的数据传输
 *
 * @author RK-Web
 * @since 2026/03/25
 */
@Data
@ApiModel(description = "新闻审核通过DTO")
public class NewsApproveDTO {

    @ApiModelProperty(value = "新闻ID", required = true)
    @NotNull(message = "新闻ID不能为空")
    private Long id;

    @ApiModelProperty("审核备注")
    private String remark;

    @ApiModelProperty("是否通过")
    private Boolean approved;
}
