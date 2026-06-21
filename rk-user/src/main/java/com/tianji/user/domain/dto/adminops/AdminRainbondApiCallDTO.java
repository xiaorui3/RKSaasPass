package com.tianji.user.domain.dto.adminops;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AdminRainbondApiCallDTO {
    private String method;
    private String path;
    private Map<String, Object> query = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private Object body;
}
