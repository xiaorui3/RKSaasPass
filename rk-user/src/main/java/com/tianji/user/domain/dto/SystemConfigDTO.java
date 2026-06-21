package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 系统配置DTO
 *
 * @author RK-Web
 * @since 2026-03-29
 */
@Data
@ApiModel(description = "系统配置表单数据")
public class SystemConfigDTO {

    @ApiModelProperty(value = "配置键", required = true)
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @ApiModelProperty(value = "配置值", required = true)
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    @ApiModelProperty(value = "配置描述")
    private String description;

    @ApiModelProperty(value = "是否启用：0-禁用，1-启用", required = true)
    @NotNull(message = "是否启用不能为空")
    private Boolean isEnabled;
}
