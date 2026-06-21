package com.tianji.data.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataQualityRepairRequestDTO {
    private Long tenantId;
    private String issueType;
    private String reason;
}
