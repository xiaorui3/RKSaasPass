package com.tianji.auth.domain.vo;

import com.tianji.auth.domain.po.AccountRole;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 登录响应VO
 */
@Data
@ApiModel("登录响应VO")
public class LoginVO {

    @ApiModelProperty("访问令牌")
    private String token;

    @ApiModelProperty("刷新令牌")
    private String refreshToken;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("用户头像")
    private String avatar;

    @ApiModelProperty("用户头像 URL")
    private String avatarUrl;

    @ApiModelProperty("兼容前端头像字段")
    private String icon;

    @ApiModelProperty("租户ID")
    private String organizationId;

    @ApiModelProperty("用户类型")
    private String userType;

    @ApiModelProperty("过期时间（秒）")
    private Long expiresIn;

    @ApiModelProperty("角色列表")
    private List<AccountRole> roles;
}
