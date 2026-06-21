package com.tianji.api.dto.user;

import com.tianji.common.constants.RegexConstants;
import com.tianji.common.validate.annotations.EnumValid;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "User detail")
public class UserDTO {
    @ApiModelProperty(value = "local user id", example = "1")
    private Long id;

    @ApiModelProperty(value = "auth user id", example = "1")
    private Long authUserId;

    @ApiModelProperty(value = "tenant id", example = "1")
    private Long tenantId;

    @ApiModelProperty(value = "phone", example = "13890011009")
    @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "invalid cellphone")
    private String cellPhone;

    @ApiModelProperty(value = "display name", example = "张三")
    private String name;

    @ApiModelProperty(value = "user type", example = "2")
    @EnumValid(enumeration = {1, 2, 3}, message = "invalid user type")
    @NotNull
    private Integer type;

    @ApiModelProperty(value = "role id", example = "5")
    private Long roleId;

    @ApiModelProperty(value = "role name")
    private String roleName;

    @ApiModelProperty(value = "icon")
    private String icon;

    @ApiModelProperty(value = "job")
    private String job;

    @ApiModelProperty(value = "intro")
    private String intro;

    @ApiModelProperty(value = "photo")
    private String photo;

    @ApiModelProperty(value = "username", example = "13800010004")
    private String username;

    @ApiModelProperty(value = "status")
    private Integer status;

    @ApiModelProperty(value = "email")
    @Email
    private String email;

    @ApiModelProperty(value = "student id")
    private String studentId;

    @ApiModelProperty(value = "college")
    private String college;

    @ApiModelProperty(value = "major")
    private String major;

    @ApiModelProperty(value = "grade")
    private String grade;

    @ApiModelProperty(value = "department")
    private String department;

    @ApiModelProperty(value = "position")
    private String position;

    @ApiModelProperty(value = "join date")
    private LocalDateTime joinDate;

    @ApiModelProperty(value = "qq")
    private String qq;

    @ApiModelProperty(value = "province")
    private String province;

    @ApiModelProperty(value = "city")
    private String city;

    @ApiModelProperty(value = "district")
    private String district;

    @ApiModelProperty(value = "gender", example = "0")
    @EnumValid(enumeration = {0, 1}, message = "invalid gender")
    private Integer gender;
}
