package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("邮件中心收件人解析请求")
public class EmailCenterRecipientResolveDTO {

    @ApiModelProperty("租户ID，内部调用时可显式传入")
    private Long tenantId;

    @ApiModelProperty("角色ID列表")
    private List<Long> roleIds;

    @ApiModelProperty("认证用户ID列表")
    private List<Long> authUserIds;

    @ApiModelProperty("用户名列表")
    private List<String> usernames;

    @ApiModelProperty("活动ID")
    private Long activityId;

    @ApiModelProperty("比赛ID")
    private Long competitionId;
}
