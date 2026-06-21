package com.tianji.user.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class ApiCatalogVO {
    private LocalDateTime generatedAt;
    private String tenantScope;
    private Integer serviceCount;
    private Integer endpointCount;
    private Integer publicApiCount;
    private Integer adminApiCount;
    private Integer internalApiCount;
    private List<ServiceGroup> services = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class ServiceGroup {
        private String name;
        private String sourceUrl;
        private String basePath;
        private Integer endpointCount;
        private List<Endpoint> endpoints = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class Endpoint {
        private String serviceName;
        private String method;
        private String path;
        private String summary;
        private String operationId;
        private String tag;
        private String category;
        private String permissionPoint;
        private String sampleQueryJson;
        private String sampleBodyJson;
        private Boolean deprecated;
        private List<String> consumes = new ArrayList<>();
        private List<String> produces = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class DebugResult {
        private String method;
        private String path;
        private Integer statusCode;
        private String body;
        private Map<String, String> responseHeaders = new LinkedHashMap<>();
        private LocalDateTime calledAt;
    }
}
