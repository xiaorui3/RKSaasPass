package com.tianji.api.dto.user;

import lombok.Data;

@Data
public class TenantWorkflowConfigDTO {
    private WorkflowPolicyDTO registration;
    private WorkflowPolicyDTO joinReview;
    private WorkflowPolicyDTO activityPublish;
    private WorkflowPolicyDTO competitionPublish;
    private WorkflowPolicyDTO newsPublish;
    private WorkflowPolicyDTO activitySignup;
    private WorkflowPolicyDTO competitionSignup;
}
