package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class GrowthRetentionCenterVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private MonthlyTrend monthlyTrend = new MonthlyTrend();
    private List<MetricItem> metrics = new ArrayList<>();
    private List<TrendItem> trendRows = new ArrayList<>();
    private List<RiskItem> riskRows = new ArrayList<>();
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
        private String timeRange = "90D";
        private String riskLevel = "ALL";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long newMemberCount;
        private long activeMemberCount;
        private long dormantMemberCount;
        private long lostMemberCount;
        private long activeClubCount;
        private double retentionRate;
        private double volunteerHours;
        private double creditTotal;
        private long creditRecordCount;
    }

    @Data
    @Accessors(chain = true)
    public static class MonthlyTrend {
        private long totalNewMemberCount;
        private long totalActiveMemberCount;
        private long totalDormantMemberCount;
        private double totalVolunteerHours;
        private double totalCreditTotal;
    }

    @Data
    @Accessors(chain = true)
    public static class MetricItem {
        private String metric;
        private String label;
        private double value;
        private String hint;
        private String source;
    }

    @Data
    @Accessors(chain = true)
    public static class TrendItem {
        private String month;
        private long newMemberCount;
        private long activeMemberCount;
        private long dormantMemberCount;
        private double volunteerHours;
        private double creditTotal;
    }

    @Data
    @Accessors(chain = true)
    public static class RiskItem {
        private String riskType;
        private String title;
        private String riskLevel;
        private String status;
        private String source;
        private String suggestion;
        private LocalDateTime detectedAt;
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
