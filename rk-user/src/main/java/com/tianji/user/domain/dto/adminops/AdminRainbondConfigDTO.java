package com.tianji.user.domain.dto.adminops;

import lombok.Data;

@Data
public class AdminRainbondConfigDTO {
    private Boolean enabled;
    private String baseUrl;
    private String token;
    private String enterpriseId;
    private String teamId;
    private String regionName;
    private Integer timeoutSeconds;
}
