package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminJenkinsGitBranchesVO {
    private String source;
    private String label;
    private String repositoryUrl;
    private List<String> branches;
    private Boolean fallback;
    private String failureReason;
    private String message;
}
