package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MobileReleaseConfigVO {
    private Long configId;
    private boolean enabled;
    private boolean hasUpdate;
    private boolean forceUpgrade;
    private String versionName;
    private Integer versionCode;
    private Integer minSupportedVersionCode;
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
    private List<MobileReleaseHistoryVO> history = new ArrayList<>();
    private String message;
}
