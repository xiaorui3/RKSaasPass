package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MobileReleaseHistoryVO {
    private String historyId;
    private String releaseSource;
    private String status;
    private Long buildId;
    private String versionName;
    private Integer versionCode;
    private Integer minSupportedVersionCode;
    private Boolean forceUpgrade;
    private String downloadUrl;
    private String apkPath;
    private Long fileSize;
    private String releaseNotes;
    private String gitCommit;
    private String gitRange;
    private String targetMode;
    private List<Long> tenantIds = new ArrayList<>();
    private List<Long> roleIds = new ArrayList<>();
    private String buildStatus;
    private String buildLog;
    private String createdAt;
}
