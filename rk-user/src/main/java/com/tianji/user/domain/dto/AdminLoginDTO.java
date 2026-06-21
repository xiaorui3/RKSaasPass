package com.tianji.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 管理员登录数据传输对象
 */
@Data
public class AdminLoginDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 管理员密钥
     */
    private String adminKey;

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 验证码key
     */
    private String captchaKey;
}
