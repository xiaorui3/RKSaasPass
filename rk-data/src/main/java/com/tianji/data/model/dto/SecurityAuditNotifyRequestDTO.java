package com.tianji.data.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SecurityAuditNotifyRequestDTO {
    private Long tenantId;
    private String riskType;
    private String riskLevel;
    private String reason;
    private String audience;
    private String operator;
}
