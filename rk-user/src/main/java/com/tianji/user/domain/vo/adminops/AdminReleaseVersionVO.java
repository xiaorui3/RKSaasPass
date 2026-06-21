package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminReleaseVersionVO {
    private Long id;
    private String serviceCode;
    private String versionTag;
    private String sourceImage;
    private String registryImage;
    private String sourceType;
    private String gitSource;
    private String branchName;
    private String commitId;
    private Long jenkinsRecordId;
    private Integer buildNumber;
    private String status;
    private String createTime;
}
