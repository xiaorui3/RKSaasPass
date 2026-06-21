package com.tianji.api.dto.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkflowPolicyDTO {
    private Boolean openRegistration;
    private Boolean requireApproval;
    private Boolean notifyAdmins;
    private Boolean notifyApplicantOnFailure;
    private Boolean notifyOnSuccess;
    private String advisorMode;
    private List<Long> designatedAdvisorRoleIds = new ArrayList<>();
}
