package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminApiWorkbenchDocVO {
    private String sourceUrl;
    private String serviceName;
    private String basePath;
    private List<AdminApiWorkbenchEndpointVO> endpoints;
}
