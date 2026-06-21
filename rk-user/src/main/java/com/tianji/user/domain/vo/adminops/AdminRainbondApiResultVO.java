package com.tianji.user.domain.vo.adminops;

import lombok.Data;

@Data
public class AdminRainbondApiResultVO {
    private String method;
    private String path;
    private String requestUrl;
    private Integer statusCode;
    private Boolean success;
    private String responseBody;
    private Long durationMillis;
    private String message;
}
