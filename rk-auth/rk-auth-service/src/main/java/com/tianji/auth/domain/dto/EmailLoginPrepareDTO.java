package com.tianji.auth.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel("邮箱验证码登录预处理DTO")
public class EmailLoginPrepareDTO {

    @ApiModelProperty(value = "邮箱", required = true)
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    @ApiModelProperty("可选租户过滤")
    private Long tenantId;
}
