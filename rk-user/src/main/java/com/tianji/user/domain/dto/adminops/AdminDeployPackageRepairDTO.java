package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminDeployPackageRepairDTO {
    private String repairAction;
    private String confirmText;
    private String reason;
}
