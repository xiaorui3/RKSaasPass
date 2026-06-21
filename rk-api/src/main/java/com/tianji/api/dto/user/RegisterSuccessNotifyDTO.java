package com.tianji.api.dto.user;

import lombok.Data;

import java.util.Map;

@Data
public class RegisterSuccessNotifyDTO {
    private Long tenantId;
    private Long authUserId;
    private String username;
    private String name;
    private String email;
    private String referralCode;
    private Map<String, Object> formPayload;
}
