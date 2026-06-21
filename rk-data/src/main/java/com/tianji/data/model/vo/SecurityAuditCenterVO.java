package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class SecurityAuditCenterVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private List<RiskItem> risks = new ArrayList<>();
    private List<NotifyLog> notifyLogs = new ArrayList<>();
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
        private String riskType = "ALL";
        private String riskLevel = "ALL";
        private String notifyStatus = "ALL";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long totalRiskCount;
        private long crossTenantCount;
        private long highRiskOperationCount;
        private long abnormalLoginCount;
        private long permissionChangeCount;
        private long notifyPendingCount;
        private long notifySentCount;
    }

    @Data
    @Accessors(chain = true)
    public static class RiskItem {
        private String riskType;
        private String riskTypeLabel;
        private String title;
        private String description;
        private String riskLevel;
        private String status;
        private String source;
        private String sourceId;
        private String sourcePath;
        private String actorName;
        private String loginLocation;
        private String menuName;
        private String createdAt;
        private String updatedAt;
        private boolean notifyable;
        private boolean menuPermissionLinked;
        private String notifyStatus;
    }

    @Data
    @Accessors(chain = true)
    public static class NotifyLog {
        private Long tenantId;
        private String riskType;
        private String riskLevel;
        private String status;
        private String audience;
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
