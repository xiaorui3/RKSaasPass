package com.tianji.auth.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@ApiModel("用户注册DTO")
public class RegisterDTO {

    @ApiModelProperty(value = "用户名", required = true)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("租户ID")
    private String organizationId;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("邮箱验证码")
    private String emailCode;

    @ApiModelProperty("邀请token")
    private String inviteToken;

    @ApiModelProperty("内推码")
    private String referralCode;

    @ApiModelProperty("租户动态表单完整数据")
    private Map<String, Object> formPayload;
}
