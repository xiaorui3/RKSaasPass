package com.tianji.auth.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("邮箱登录候选账号")
public class EmailLoginCandidateVO {

    @ApiModelProperty("认证账号ID")
    private Long authUserId;

    @ApiModelProperty("租户ID")
    private Long tenantId;

    private Long roleId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("显示名称")
    private String displayName;

    @ApiModelProperty("头像")
    private String avatar;
}
