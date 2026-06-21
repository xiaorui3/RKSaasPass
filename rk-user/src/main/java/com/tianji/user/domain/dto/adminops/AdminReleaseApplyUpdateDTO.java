package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminReleaseApplyUpdateDTO {
    private String manifestJson;
    private Boolean dryRun;
    private String runtimeMode;
    private String confirmText;
}
