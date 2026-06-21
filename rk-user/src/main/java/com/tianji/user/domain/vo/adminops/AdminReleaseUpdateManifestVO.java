package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminReleaseUpdateManifestVO {
    private Long packageId;
    private String updateVersion;
    private String channel;
    private String runtimeMode;
    private String manifestJson;
    private Integer actionCount;
    private String status;
}
