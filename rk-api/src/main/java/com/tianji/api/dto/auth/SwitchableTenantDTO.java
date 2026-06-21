package com.tianji.api.dto.auth;

import lombok.Data;

@Data
public class SwitchableTenantDTO {
    private Long tenantId;
    private Long authUserId;
    private String username;
    private Long roleId;
}
