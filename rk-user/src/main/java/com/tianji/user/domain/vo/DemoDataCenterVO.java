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
public class DemoDataCenterVO {
    private String status;
    private String safeTag;
    private String demoBatchId;
    private String demoTenantCode;
    private Long tenantId;
    private Boolean repeatable;
    private Boolean cleanupSupported;
    private LocalDateTime generatedAt;
    private LocalDateTime cleanedAt;
    private LocalDateTime updatedAt;
    private List<String> modules = new ArrayList<>();
    private Map<String, Integer> counts = new LinkedHashMap<>();
    private List<SampleItem> sampleItems = new ArrayList<>();
    private List<ModuleStatus> moduleStatuses = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class SampleItem {
        private String module;
        private String title;
        private String description;
        private String status;
        private String source;
    }

    @Data
    @Accessors(chain = true)
    public static class ModuleStatus {
        private String module;
        private String title;
        private String status;
        private Integer count;
        private String message;
    }
}
