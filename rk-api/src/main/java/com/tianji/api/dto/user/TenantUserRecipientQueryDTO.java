package com.tianji.api.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class TenantUserRecipientQueryDTO {
    private Long tenantId;
    private List<Long> authUserIds;
    private List<String> usernames;
}
