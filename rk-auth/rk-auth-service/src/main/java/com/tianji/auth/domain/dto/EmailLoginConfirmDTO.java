package com.tianji.auth.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("邮箱验证码登录确认DTO")
public class EmailLoginConfirmDTO {

    @ApiModelProperty(value = "登录票据", required = true)
    @NotBlank(message = "登录票据不能为空")
    private String loginTicket;

    @ApiModelProperty(value = "认证账号ID", required = true)
    @NotNull(message = "认证账号ID不能为空")
    private Long authUserId;

    @ApiModelProperty(value = "租户ID", required = true)
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    private Long roleId;
}
