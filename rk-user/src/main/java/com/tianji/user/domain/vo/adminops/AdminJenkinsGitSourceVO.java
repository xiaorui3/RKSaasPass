package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminJenkinsGitSourceVO {
    private String source;
    private String label;
    private String repositoryUrl;
    private String defaultBranch;
    private Boolean defaultSource;
}
