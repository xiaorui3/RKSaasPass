package com.tianji.message.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "邮件发送参数")
public class EmailInfoDTO {

    @ApiModelProperty("主题")
    private String subject;

    @ApiModelProperty("收件邮箱")
    private Iterable<String> emails;

    @ApiModelProperty("邮件内容")
    private String content;

    @ApiModelProperty("是否 HTML")
    private Boolean html;

    @ApiModelProperty("发送任务ID")
    private Long taskId;

    @ApiModelProperty("收件人快照ID列表")
    private List<Long> recipientIds;
}
