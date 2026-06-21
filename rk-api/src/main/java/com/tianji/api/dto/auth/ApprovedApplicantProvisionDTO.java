package com.tianji.api.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("审核通过申请人建号DTO")
public class ApprovedApplicantProvisionDTO {

    @ApiModelProperty("租户ID")
    @NotNull
    private Long tenantId;

    @ApiModelProperty("申请账号")
    @NotBlank
    private String username;

    @ApiModelProperty("已加密密码")
    private String encodedPassword;

    @ApiModelProperty("来源认证用户ID（已登录跨租户申请时使用）")
    private Long sourceAuthUserId;
}
