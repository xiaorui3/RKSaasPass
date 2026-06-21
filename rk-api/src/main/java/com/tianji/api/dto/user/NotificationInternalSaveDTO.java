package com.tianji.api.dto.user;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NotificationInternalSaveDTO {
    private Long tenantId;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private Integer targetType;
    private List<Long> targetIds;
    private String senderName;
    private Map<String, Object> metadata;
}
