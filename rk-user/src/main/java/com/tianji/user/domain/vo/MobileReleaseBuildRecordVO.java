package com.tianji.user.domain.vo;

import lombok.Data;

@Data
public class MobileReleaseBuildRecordVO {
    private Long id;
    private String status;
    private String versionName;
    private Integer versionCode;
    private Integer minSupportedVersionCode;
    private Boolean forceUpgrade;
    private String downloadUrl;
    private String apkPath;
    private Long fileSize;
    private String releaseNotes;
    private String releaseSource;
    private String gitCommit;
    private String gitRange;
    private String buildLog;
    private String startedAt;
    private String finishedAt;
    private Long durationMillis;
    private String createdAt;
}
