package com.tianji.message.api.dto;

import lombok.Data;

@Data
public class EmailTemplateDTO {
    private Long id;
    private String templateCode;
    private String templateName;
    private String emailSubject;
    private String templateContent;
    private Boolean htmlMode;
    private String mediaPayload;
    private Integer status;
    private String createTime;
}
