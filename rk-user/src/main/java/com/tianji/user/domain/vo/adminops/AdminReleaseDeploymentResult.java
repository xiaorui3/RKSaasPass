package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminReleaseDeploymentResult {
    private boolean success;
    private String currentStep;
    private Integer progress;
    private String logs;

    public static AdminReleaseDeploymentResult success(String currentStep, Integer progress, String logs) {
        AdminReleaseDeploymentResult result = new AdminReleaseDeploymentResult();
        result.setSuccess(true);
        result.setCurrentStep(currentStep);
        result.setProgress(progress);
        result.setLogs(logs);
        return result;
    }

    public static AdminReleaseDeploymentResult failed(String currentStep, Integer progress, String logs) {
        AdminReleaseDeploymentResult result = new AdminReleaseDeploymentResult();
        result.setSuccess(false);
        result.setCurrentStep(currentStep);
        result.setProgress(progress);
        result.setLogs(logs);
        return result;
    }
}
