package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminTrafficOnlineUserVO {
    private Long tenantId;
    private String tenantName;
    private String userName;
    private String userDisplayName;
    private String deviceType;
    private String clientType;
    private String ip;
    private String country;
    private String province;
    private String city;
    private String lastSeenAt;
    private Long activityCount;
    private String samplePath;
    private String sampleAction;
    private String rawLocation;
    private Double lat;
    private Double lng;
}
