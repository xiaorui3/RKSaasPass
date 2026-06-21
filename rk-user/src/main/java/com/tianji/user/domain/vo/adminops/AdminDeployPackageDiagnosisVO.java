package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminDeployPackageDiagnosisVO {
    private Long packageId;
    private String namespace;
    private Boolean success;
    private String diagnosedAt;
    private List<Map<String, Object>> serviceStatuses;
    private List<Map<String, Object>> diagnosisChecks;
    private List<String> suggestedRepairActions;
    private String terminalLog;
}
