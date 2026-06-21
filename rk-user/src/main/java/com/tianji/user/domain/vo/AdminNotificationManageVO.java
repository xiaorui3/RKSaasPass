package com.tianji.user.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminNotificationManageVO {
    private Long id;
    private Long tenantId;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private Integer targetType;
    private List<Long> targetIds;
    private String senderName;
    private String status;
    private Integer readCount;
    private String createTime;
    private String updateTime;
    private String upgradeVersion;
    private Boolean forceUpgrade;
    private String downloadUrl;
    private String releaseNotes;
    private Map<String, Object> metadata;
}
