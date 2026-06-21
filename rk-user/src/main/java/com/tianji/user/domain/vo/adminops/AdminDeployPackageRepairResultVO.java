package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminDeployPackageRepairResultVO {
    private Long packageId;
    private String repairAction;
    private Boolean success;
    private String repairedAt;
    private List<String> outputs;
    private String nextSuggestion;
    private Long auditId;
}
