package com.tianji.user.domain.dto;

import lombok.Data;

@Data
public class NotificationDeliveryConfigDTO {
    private Long tenantId;
    private Boolean enabled;
    private Boolean systemEnabled;
    private Boolean messageEnabled;
    private Boolean activityEnabled;
    private Boolean upgradeEnabled;
}
