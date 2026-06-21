package com.tianji.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("邮件邀请发送请求")
public class EmailInvitationSendDTO {

    @ApiModelProperty("手工邮箱列表")
    private List<String> manualEmails;

    @ApiModelProperty("邀请类型：REGISTER / JOIN")
    private String invitationType;

    @ApiModelProperty("邮件主题")
    private String subject;

    @ApiModelProperty("邮件正文")
    private String content;

    @ApiModelProperty("绑定内推码")
    private String referralCode;
}
