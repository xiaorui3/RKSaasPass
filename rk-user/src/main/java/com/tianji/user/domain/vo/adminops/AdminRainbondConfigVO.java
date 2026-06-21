package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminRainbondConfigVO {
    private Boolean enabled;
    private String baseUrl;
    private String tokenMasked;
    private Boolean tokenConfigured;
    private String enterpriseId;
    private String teamId;
    private String regionName;
    private Integer timeoutSeconds;
    private String message;
}
