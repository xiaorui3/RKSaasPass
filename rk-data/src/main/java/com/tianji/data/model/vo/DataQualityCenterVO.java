package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class DataQualityCenterVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private List<IssueItem> issues = new ArrayList<>();
    private List<RepairLog> repairLogs = new ArrayList<>();
    private List<SourceInfo> sources = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    public static class CacheInfo {
        private boolean hit;
        private String mode = "computed";
        private String key;
        private Integer ttlSeconds;
        private LocalDateTime cachedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class Filters {
        private String issueType = "ALL";
        private String riskLevel = "ALL";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long totalIssueCount;
        private long highRiskCount;
        private long repairableCount;
        private long orphanUserCount;
        private long missingRoleCount;
        private long missingMediaCount;
        private long searchStaleCount;
        private long redisRiskCount;
        private long driftCount;
    }

    @Data
    @Accessors(chain = true)
    public static class IssueItem {
        private String issueType;
        private String issueTypeLabel;
        private String title;
        private String description;
        private String riskLevel;
        private String status;
        private String source;
        private String sourceId;
        private String suggestion;
        private boolean repairable;
        private String repairAction;
        private String detectedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class RepairLog {
        private Long tenantId;
        private String issueType;
        private String action;
        private String status;
        private String reason;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @Accessors(chain = true)
    public static class SourceInfo {
        private String metric;
        private String tableName;
        private String status;
        private LocalDateTime updatedAt;
    }
}
