package com.tianji.user.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminNotificationSaveDTO {
    private Long id;
    private Long tenantId;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private Integer targetType;
    private List<Long> targetIds;
    private String senderName;
    private String upgradeVersion;
    private Boolean forceUpgrade;
    private String downloadUrl;
    private String releaseNotes;
    private Map<String, Object> metadata;
}
