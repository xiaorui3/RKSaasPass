package com.tianji.data.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class ParticipationAnalysisCenterVO {
    private Long tenantId;
    private String tenantScope;
    private LocalDateTime updatedAt;
    private CacheInfo cache = new CacheInfo();
    private Filters filters = new Filters();
    private Summary summary = new Summary();
    private Trend trend = new Trend();
    private Trend engagementTrend = new Trend();
    private List<MetricItem> metrics = new ArrayList<>();
    private List<TrendItem> trendRows = new ArrayList<>();
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
        private String dimension = "ALL";
        private String timeRange = "30D";
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private long viewCount;
        private long activityRegistrationCount;
        private long competitionRegistrationCount;
        private long commentCount;
        private long followCount;
        private long revisitCount;
        private long activeMemberCount;
        private double registrationConversionRate;
        private double interactionRate;
    }

    @Data
    @Accessors(chain = true)
    public static class Trend {
        private long totalViewCount;
        private long totalRegistrationCount;
        private long totalCommentCount;
        private long totalFollowCount;
        private long totalRevisitCount;
    }

    @Data
    @Accessors(chain = true)
    public static class MetricItem {
        private String metric;
        private String label;
        private long value;
        private String hint;
        private String source;
    }

    @Data
    @Accessors(chain = true)
    public static class TrendItem {
        private String dimension;
        private String label;
        private long viewCount;
        private long registrationCount;
        private long commentCount;
        private long followCount;
        private long revisitCount;
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
