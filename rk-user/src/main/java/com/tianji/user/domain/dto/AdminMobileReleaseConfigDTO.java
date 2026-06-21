package com.tianji.user.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminMobileReleaseConfigDTO {
    private Boolean enabled;
    private String versionName;
    private Integer versionCode;
    private Integer minSupportedVersionCode;
    private Boolean forceUpgrade;
    private String downloadUrl;
    private String apkPath;
    private Long fileSize;
    private String releaseNotes;
    private String releaseSource;
    private String buildStatus;
    private String buildLog;
    private Long buildId;
    private String gitCommit;
    private String gitRange;
    private String createdAt;
    private String targetMode;
    private List<Long> tenantIds = new ArrayList<>();
    private List<Long> roleIds = new ArrayList<>();
    private List<com.tianji.user.domain.vo.MobileReleaseHistoryVO> history = new ArrayList<>();
}
