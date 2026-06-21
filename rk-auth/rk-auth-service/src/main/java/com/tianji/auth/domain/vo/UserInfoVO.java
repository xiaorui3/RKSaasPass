package com.tianji.auth.domain.vo;

import com.tianji.auth.domain.po.AccountRole;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息VO
 */
@Data
@ApiModel("用户信息VO")
public class UserInfoVO {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("学号")
    private String studentId;

    @ApiModelProperty("专业")
    private String major;

    @ApiModelProperty("年级")
    private String grade;

    @ApiModelProperty("个人简介")
    private String bio;

    @ApiModelProperty("租户ID")
    private String organizationId;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("角色列表")
    private List<AccountRole> roles;
}