package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminReleaseRegistryPublishResult {
    private boolean success;
    private String currentStep;
    private Integer progress;
    private String logs;

    public static AdminReleaseRegistryPublishResult success(String currentStep, Integer progress, String logs) {
        AdminReleaseRegistryPublishResult result = new AdminReleaseRegistryPublishResult();
        result.setSuccess(true);
        result.setCurrentStep(currentStep);
        result.setProgress(progress);
        result.setLogs(logs);
        return result;
    }

    public static AdminReleaseRegistryPublishResult failed(String currentStep, Integer progress, String logs) {
        AdminReleaseRegistryPublishResult result = new AdminReleaseRegistryPublishResult();
        result.setSuccess(false);
        result.setCurrentStep(currentStep);
        result.setProgress(progress);
        result.setLogs(logs);
        return result;
    }

    @Data
    public static class PublishProgress {
        private String currentStep;
        private Integer progress;

        public PublishProgress(String currentStep, Integer progress) {
            this.currentStep = currentStep;
            this.progress = progress;
        }
    }
}
