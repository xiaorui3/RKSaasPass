package com.tianji.message.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "邮件模板")
public class EmailTemplateDTO {

    @ApiModelProperty("模板ID")
    private Long id;

    @ApiModelProperty("模板编码")
    private String templateCode;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("邮件主题")
    private String emailSubject;

    @ApiModelProperty("模板正文")
    private String templateContent;

    @ApiModelProperty("是否HTML")
    private Boolean htmlMode;

    @ApiModelProperty("媒体配置JSON")
    private String mediaPayload;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("创建时间")
    private String createTime;
}
