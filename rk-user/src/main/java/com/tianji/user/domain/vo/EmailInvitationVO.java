package com.tianji.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("邮件邀请详情")
public class EmailInvitationVO {

    @ApiModelProperty("记录ID")
    private Long id;

    @ApiModelProperty("租户ID")
    private Long tenantId;

    @ApiModelProperty("目标邮箱")
    private String targetEmail;

    @ApiModelProperty("邀请类型")
    private String invitationType;

    @ApiModelProperty("绑定内推码")
    private String referralCode;

    @ApiModelProperty("是否已接受")
    private Boolean accepted;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("转化状态")
    private String conversionStatus;

    @ApiModelProperty("创建时间")
    private String createTime;
}
