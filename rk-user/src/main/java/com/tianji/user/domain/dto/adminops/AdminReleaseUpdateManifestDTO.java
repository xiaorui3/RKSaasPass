package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminReleaseUpdateManifestDTO {
    private String updateVersion;
    private String channel;
    private String runtimeMode;
    private String commitId;
    private List<String> serviceCodes;
    private String note;
}
