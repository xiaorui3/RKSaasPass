package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.Map;

@Data
public class AdminDeployPackageDetailVO {
    private AdminDeployPackageRecordVO record;
    private Map<String, Object> requestSummary;
    private Map<String, Object> lastDiagnosis;
    private Map<String, Object> lastRepair;
    private String kubeconfigRedacted;
    private String kubeconfigFingerprint;
}
