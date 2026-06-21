package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminReleasePublishCurrentDTO {
    private String currentImage;
    private String registryPrefix;
    private String versionTag;
    private Boolean dryRun;
    private String reason;
}
