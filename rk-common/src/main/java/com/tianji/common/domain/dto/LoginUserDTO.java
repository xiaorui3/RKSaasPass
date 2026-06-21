package com.tianji.common.domain.dto;

import lombok.Data;

@Data
public class LoginUserDTO {
    private Long userId;
    /**
     * 租户ID（企业级多租户标准）
     * 用于数据隔离和权限控制
     */
    private Long tenantId;
    private Long roleId;
    private Boolean rememberMe;
    private String username;
    private String clientType;
    /**
     * 组织ID（旧标识，保留兼容性）
     * @deprecated 建议使用tenantId
     */
    @Deprecated
    private String organizationId;
}
