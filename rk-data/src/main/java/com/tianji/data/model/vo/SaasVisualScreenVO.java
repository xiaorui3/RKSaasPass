package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class SaasVisualScreenVO {
    private Long tenantId;
    private String tenantScope;
    private String tenantName;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private List<SourceInfo> sources = new ArrayList<>();
    private TenantOverview tenantOverview = new TenantOverview();
    private PeopleOverview people = new PeopleOverview();
    private ContentOverview content = new ContentOverview();
    private ActivityOverview activity = new ActivityOverview();
    private ApprovalOverview approvals = new ApprovalOverview();
    private InteractionOverview interactions = new InteractionOverview();
    private GrowthOverview growth = new GrowthOverview();
    private FinanceOverview finance = new FinanceOverview();
    private NotificationOverview notifications = new NotificationOverview();
    private CacheSnapshot cacheSnapshot = new CacheSnapshot();
    private SearchHealth searchHealth = new SearchHealth();
    private ServiceHealth serviceHealth = new ServiceHealth();

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
    public static class TenantOverview {
        private long tenantCount;
        private long activeTenantCount;
        private long currentTenantUsers;
    }

    @Data
    @Accessors(chain = true)
    public static class PeopleOverview {
        private long memberCount;
        private long roleCount;
        private long onlineCount;
        private long todayLoginCount;
    }

    @Data
    @Accessors(chain = true)
    public static class ContentOverview {
        private long newsCount;
        private long noticeCount;
        private long todayPublishedCount;
    }

    @Data
    @Accessors(chain = true)
    public static class ActivityOverview {
        private long activityCount;
        private long competitionCount;
        private long registrationCount;
        private long todayRegistrationCount;
    }

    @Data
    @Accessors(chain = true)
    public static class ApprovalOverview {
        private long joinPendingCount;
        private long contentPendingCount;
        private long financePendingCount;
        private long totalPendingCount;
    }

    @Data
    @Accessors(chain = true)
    public static class InteractionOverview {
        private long commentCount;
        private long todayCommentCount;
        private long contactMessageCount;
    }

    @Data
    @Accessors(chain = true)
    public static class GrowthOverview {
        private double volunteerHours;
        private double creditTotal;
        private long creditRecordCount;
    }

    @Data
    @Accessors(chain = true)
    public static class FinanceOverview {
        private double budgetAmount;
        private double reimbursementAmount;
        private long reimbursementPendingCount;
    }

    @Data
    @Accessors(chain = true)
    public static class NotificationOverview {
        private long notificationCount;
        private long emailTaskCount;
        private long todayReachCount;
    }

    @Data
    @Accessors(chain = true)
    public static class CacheSnapshot {
        private long totalKeys;
        private long keyCount;
        private long totalBytes;
        private String totalSizeText = "0 B";
        private long hitCount;
        private long nullHitCount;
        private long nullMarkerCount;
        private long warmupKeyCount;
        private long normalKeyCount;
        private long randomExpireCount;
        private boolean antiPenetrationEnabled = true;
        private boolean randomTtlEnabled = true;
    }

    @Data
    @Accessors(chain = true)
    public static class SearchHealth {
        private String indexName = "rk_global_search";
        private String indexStatus = "UNKNOWN";
        private long documentCount;
        private long staleDocumentCount;
        private LocalDateTime lastIndexedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class ServiceHealth {
        private String status = "UNKNOWN";
        private String message = "business aggregation available";
        private LocalDateTime checkedAt;
    }
}
