package com.tianji.api.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel("Email verification verify DTO")
public class EmailVerificationVerifyDTO {

    @ApiModelProperty(value = "Email", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ApiModelProperty("Tenant ID")
    private Long tenantId;

    @ApiModelProperty(value = "Scene", required = true)
    @NotBlank(message = "场景不能为空")
    private String scene;

    @ApiModelProperty(value = "Code", required = true)
    @NotBlank(message = "验证码不能为空")
    private String code;
}
