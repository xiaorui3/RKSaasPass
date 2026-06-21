package com.tianji.api.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("按角色查询收件人请求")
public class RoleRecipientQueryDTO {

    @ApiModelProperty("租户ID")
    private Long tenantId;

    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;
}
