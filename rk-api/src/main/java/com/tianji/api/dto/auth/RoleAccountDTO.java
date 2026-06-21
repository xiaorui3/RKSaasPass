package com.tianji.api.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("角色关联账号信息")
public class RoleAccountDTO {

    @ApiModelProperty("账号ID")
    private Long accountId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("租户ID")
    private Long tenantId;
}
