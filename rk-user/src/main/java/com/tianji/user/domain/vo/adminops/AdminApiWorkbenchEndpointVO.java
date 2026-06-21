package com.tianji.user.domain.vo.adminops;

import lombok.Data;

import java.util.List;

@Data
public class AdminApiWorkbenchEndpointVO {
    private String path;
    private String method;
    private String summary;
    private String operationId;
    private String tag;
    private Boolean deprecated;
    private List<String> consumes;
    private List<String> produces;
}
