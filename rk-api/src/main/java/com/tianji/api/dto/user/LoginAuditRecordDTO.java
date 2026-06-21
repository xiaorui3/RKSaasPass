package com.tianji.api.dto.user;

import lombok.Data;

@Data
public class LoginAuditRecordDTO {
    private String loginName;
    private String ipaddr;
    private String browser;
    private String os;
    private String status;
    private String msg;
}
