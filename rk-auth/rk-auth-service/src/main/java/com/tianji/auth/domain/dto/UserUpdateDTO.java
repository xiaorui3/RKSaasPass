package com.tianji.auth.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;

/**
 * 用户更新DTO
 */
@Data
@ApiModel("用户更新DTO")
public class UserUpdateDTO {

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "学号")
    private String studentId;

    @ApiModelProperty(value = "专业")
    private String major;

    @ApiModelProperty(value = "年级")
    private String grade;

    @ApiModelProperty(value = "个人简介")
    private String bio;

    @ApiModelProperty(value = "密码")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}