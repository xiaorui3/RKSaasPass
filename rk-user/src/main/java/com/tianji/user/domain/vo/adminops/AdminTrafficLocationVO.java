package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminTrafficLocationVO {
    private Long tenantId;
    private String tenantName;
    private String country;
    private String province;
    private String city;
    private String ip;
    private String userName;
    private String userDisplayName;
    private Long visitCount;
    private Long loginCount;
    private Long operationCount;
    private String lastSeenAt;
    private String samplePath;
    private String sampleAction;
    private String rawLocation;
    private Double lat;
    private Double lng;
}
