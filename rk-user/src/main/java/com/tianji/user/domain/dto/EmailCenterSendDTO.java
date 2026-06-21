package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("邮件中心发送请求")
public class EmailCenterSendDTO {

    @ApiModelProperty("租户ID，内部调用时必须显式传入")
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

    @ApiModelProperty("手工邮箱列表")
    private List<String> manualEmails;

    @ApiModelProperty("邮件主题")
    private String subject;

    @ApiModelProperty("邮件正文")
    private String content;

    @ApiModelProperty("是否 HTML")
    private Boolean html;
}
