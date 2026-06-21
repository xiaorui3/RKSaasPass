package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class TenantOperationsCenterVO {
    private Long tenantId;
    private String tenantScope;
    private String tenantName;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private HealthScore healthScore = new HealthScore();
    private ApprovalCenter approvalCenter = new ApprovalCenter();
    private Participation participation = new Participation();
    private DataQuality dataQuality = new DataQuality();
    private GrowthRetention growthRetention = new GrowthRetention();
    private SecurityAudit securityAudit = new SecurityAudit();
    private List<DetailItem> approvalDetails = new ArrayList<>();
    private List<DetailItem> qualityDetails = new ArrayList<>();
    private List<DetailItem> securityDetails = new ArrayList<>();
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
    public static class SourceInfo {
        private String metric;
        private String tableName;
        private String status;
        private LocalDateTime updatedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class DetailItem {
        private String type;
        private String title;
        private String risk;
        private String status;
        private String source;
        private String sourceId;
        private String sourceTime;
    }

    @Data
    @Accessors(chain = true)
    public static class HealthScore {
        private int score = 100;
        private String level = "HEALTHY";
        private long pendingPressure;
        private long qualityIssues;
        private long securityRisks;
        private long cacheRisks;
        private long searchRisks;
    }

    @Data
    @Accessors(chain = true)
    public static class ApprovalCenter {
        private long totalPendingCount;
        private long overdueCount;
        private long joinPendingCount;
        private long activityPendingCount;
        private long competitionPendingCount;
        private long contentPendingCount;
        private long financePendingCount;
        private double slaRiskRate;
    }

    @Data
    @Accessors(chain = true)
    public static class Participation {
        private long activityRegistrationCount;
        private long competitionRegistrationCount;
        private long commentCount;
        private long newsCount;
        private long noticeCount;
        private long activeMemberCount;
        private double registrationConversionRate;
    }

    @Data
    @Accessors(chain = true)
    public static class DataQuality {
        private long orphanUserCount;
        private long missingRoleCount;
        private long missingMediaCount;
        private long searchStaleCount;
        private long redisRiskCount;
        private long issueCount;
    }

    @Data
    @Accessors(chain = true)
    public static class GrowthRetention {
        private long newMemberCount;
        private long activeMemberCount;
        private long dormantMemberCount;
        private double volunteerHours;
        private double creditTotal;
    }

    @Data
    @Accessors(chain = true)
    public static class SecurityAudit {
        private long highRiskOperationCount;
        private long crossTenantAttemptCount;
        private long abnormalLoginCount;
        private long permissionChangeCount;
        private long totalRiskCount;
    }
}
